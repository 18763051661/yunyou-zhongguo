$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$base = 'http://localhost:8080'
$dir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar  = Join-Path $dir 'cookies.txt'
$jar2 = Join-Path $dir 'cookies_anon.txt'
Remove-Item $jar,$jar2 -ErrorAction SilentlyContinue
$script:pass = 0; $script:fail = 0
$script:fails = @()

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
function WJson($name, $json) {
  [System.IO.File]::WriteAllText((Join-Path $dir $name), $json, $utf8NoBom)
}
# 统一用无 BOM UTF-8 重新生成所有请求体
WJson 'recommend.json' '{"interests":["摄影","亲子"],"durationMinutes":300,"budget":300,"crowd":"亲子","energy":"轻松","province":"北京","region":"华北"}'
WJson 'login.json' '{"username":"demo","password":"demo123"}'
WJson 'badlogin.json' '{"username":"demo","password":"wrongpwd"}'
WJson 'favorite.json' '{"spotId":1}'
WJson 'comment.json' '{"spotId":10,"rating":5,"content":"鸟巢建筑非常壮观，AI讲解很专业，推荐晚上来看亮灯！"}'
WJson 'start.json' '{"routeId":1}'
WJson 'chat1.json' '{"message":"九寨沟门票多少钱，什么时候去最好"}'
WJson 'chat2.json' '{"message":"带孩子去北京玩什么"}'
WJson 'chat3.json' '{"message":"推荐一条丝绸之路路线"}'
WJson 'register.json' '{"username":"smoketest","password":"test123456","nickname":"冒烟测试员"}'
WJson 'profile.json' '{"nickname":"改名的测试员","preferenceTags":"摄影,亲子,温泉"}'

function J($s) {
  try { return ($s | ConvertFrom-Json) } catch { Write-Host "BAD JSON: $($s.Substring(0,[Math]::Min(200,$s.Length)))"; return $null }
}
function G($path) {
  $raw = & curl.exe -s -b $jar "$base$path"
  return (J $raw)
}
function P($path, $file, $withAuth) {
  $curlArgs = @('-s','-H','Content-Type: application/json; charset=utf-8','--data-binary',"@$file","$base$path")
  if ($withAuth) { $curlArgs += @('-b',$jar,'-c',$jar) }
  $raw = & curl.exe @curlArgs
  return (J $raw)
}
function Check($name, $cond, $detail) {
  if ($cond) { Write-Host "PASS  $name"; $script:pass++ }
  else { Write-Host "FAIL  $name  $detail"; $script:fail++; $script:fails += $name }
}

# ---------- 全国数据概览 ----------
$r = G '/api/stats'
Check 'stats 200+ spots' ($r.code -eq 0 -and $r.data.spotCount -ge 200) "spot=$($r.data.spotCount)"
Check 'stats 31 provinces' ($r.data.provinceCount -eq 31) "pv=$($r.data.provinceCount)"
Check 'stats 16 routes' ($r.data.routeCount -ge 16) "routes=$($r.data.routeCount)"
Check 'stats region distribution=7' (@($r.data.regionDistribution).Count -eq 7)
Check 'stats province distribution=31' (@($r.data.provinceDistribution).Count -eq 31)

$r = G '/api/categories'
Check 'categories=8' ($r.code -eq 0 -and @($r.data).Count -eq 8)

# ---------- 景点分页/多维筛选 ----------
$r = G '/api/spots?page=1&size=5'
Check 'spots page size5 total200+' ($r.code -eq 0 -and @($r.data.list).Count -eq 5 -and $r.data.total -ge 200)
Check 'spot card has province' ($null -ne $r.data.list[0].province -and $null -ne $r.data.list[0].city)
$r = G '/api/spots?categoryId=1&sort=rating'
Check 'category filter+sort rating' ($r.code -eq 0 -and @($r.data.list).Count -ge 1)
$r = G '/api/spots?keyword=%E6%95%85%E5%AE%AB'
Check 'keyword 故宫' ($r.code -eq 0 -and @($r.data.list).Count -ge 1)
$r = G '/api/spots?region=%E8%A5%BF%E5%8C%97'
Check 'region 西北' ($r.code -eq 0 -and $r.data.total -ge 20) "total=$($r.data.total)"
$r = G '/api/spots?province=%E5%9B%9B%E5%B7%9D'
Check 'province 四川=10' ($r.code -eq 0 -and $r.data.total -eq 10) "total=$($r.data.total)"
$r = G '/api/spots?province=%E5%9B%9B%E5%B7%9D&freeOnly=true'
Check 'freeOnly filter' ($r.code -eq 0 -and @($r.data.list | Where-Object { $_.price -ne 0 }).Count -eq 0)
$r = G '/api/spots?sort=priceDesc&size=20'
Check 'sort priceDesc' ($r.code -eq 0 -and $r.data.list[0].price -ge $r.data.list[1].price)
$r = G '/api/spots?maxDifficulty=2&sort=difficultyAsc'
Check 'difficulty filter' ($r.code -eq 0 -and @($r.data.list).Count -ge 1)
$r = G '/api/spots/provinces'
Check 'provinces aggregate=31' ($r.code -eq 0 -and @($r.data).Count -eq 31)
$r = G '/api/spots/nearby?lng=116.40&lat=39.92&n=5'
Check 'nearby haversine' ($r.code -eq 0 -and @($r.data).Count -eq 5 -and $null -ne $r.data[0].distanceKm)
Check 'nearby nearest dist=0' ($r.data[0].distanceKm -eq 0) "$($r.data[0].distanceKm)"
$r = G '/api/spots/hot?n=12'
Check 'hot n=12' ($r.code -eq 0 -and @($r.data).Count -eq 12)
$r = G '/api/spots/top-rated?n=12'
Check 'top-rated n=12' ($r.code -eq 0 -and @($r.data).Count -eq 12)

# ---------- 详情 ----------
$r = G '/api/spots/1'
Check 'spot detail' ($r.code -eq 0 -and $r.data.name)
Check 'detail province/city' ($r.data.province -eq '北京')
Check 'detail gallery=3' (@($r.data.gallery).Count -eq 3)
Check 'spot favorited flag' ($null -ne $r.data.PSObject.Properties['favorited'])

# ---------- 路线(含经纬度) ----------
$r = G '/api/routes'
Check 'routes list' ($r.code -eq 0 -and @($r.data).Count -ge 16)
$r = G '/api/routes/1'
Check 'route detail' ($r.code -eq 0 -and @($r.data.spots).Count -ge 2)
Check 'route step lng/lat' ($r.data.spots[0].spotLng -ne 0 -and $r.data.spots[0].spotProvince)
$route = $r.data
$r = G '/api/comments?spotId=1&size=2'
Check 'comments list' ($r.code -eq 0)
if (@($r.data.list).Count -ge 1) {
  $created = [string]$r.data.list[0].createdAt
  Check 'date formatted string' ($created -match '^\d{4}-\d{2}-\d{2}') $created
} else {
  Check 'date formatted string (skip: no comment)' $true
}

# ---------- 智能推荐(返回数组, 支持目的地偏好) ----------
$r = P '/api/recommend' (Join-Path $dir 'recommend.json') $false
Check 'recommend array' ($r.code -eq 0 -and @($r.data).Count -ge 1)
Check 'recommend top1 province boost' ($r.data[0].spot.province -eq '北京') "$($r.data[0].spot.province)"

# ---------- AI 助教(真实大模型) ----------
foreach ($f in 'chat1','chat2','chat3') {
  $r = P '/api/ai/chat' (Join-Path $dir "$f.json") $false
  $txt = ($r.data | ConvertTo-Json -Compress -Depth 4)
  Check "ai $f answered" ($r.code -eq 0 -and $txt.Length -gt 20)
  Check "ai $f engine field" ($r.data.engine -in @('llm','local'))
}

# ---------- 认证 ----------
$r = P '/api/auth/login' (Join-Path $dir 'badlogin.json') $false
Check 'login wrong pwd rejected' ($null -ne $r -and $r.code -ne 0)
$r = P '/api/auth/register' (Join-Path $dir 'register.json') $false
Check 'register' ($null -ne $r)
$r2 = P '/api/auth/register' (Join-Path $dir 'register.json') $false
Check 'duplicate register graceful' ($null -ne $r2 -and $r2.code -ne 0)
$r = P '/api/auth/login' (Join-Path $dir 'login.json') $true
Check 'login demo' ($r.code -eq 0 -and $r.data.nickname -eq '小明同学')
$r = G '/api/auth/me'
Check 'me' ($r.code -eq 0 -and $r.data.username -eq 'demo')
Check 'password not leaked' ($null -eq $r.data.PSObject.Properties['password'])
$r = P '/api/auth/profile' (Join-Path $dir 'profile.json') $true
Check 'profile update' ($r.code -eq 0)
$r = G '/api/auth/me'
Check 'profile persisted' ($r.data.nickname -eq '改名的测试员' -and $r.data.preferenceTags -match '温泉')

# ---------- 收藏(与初始状态无关: 先归一化为已收藏) ----------
$r = P '/api/favorites' (Join-Path $dir 'favorite.json') $true
if ($r.data.favorited -eq $false) {
  $r = P '/api/favorites' (Join-Path $dir 'favorite.json') $true
}
Check 'favorite on' ($r.code -eq 0 -and $r.data.favorited -eq $true)
$r = G '/api/favorites'
$has1 = @($r.data | Where-Object { $_.spotId -eq 1 }).Count
Check 'favorite list contains 1' ($r.code -eq 0 -and $has1 -ge 1)
$r = P '/api/favorites' (Join-Path $dir 'favorite.json') $true
Check 'favorite toggle off' ($r.code -eq 0 -and $r.data.favorited -eq $false)
$r = G '/api/favorites'
$has1b = @($r.data | Where-Object { $_.spotId -eq 1 }).Count
Check 'favorite list removed' ($has1b -eq 0)
# 恢复种子收藏状态(demo 默认收藏 1)
$null = P '/api/favorites' (Join-Path $dir 'favorite.json') $true

# ---------- 评论 ----------
$r = P '/api/comments' (Join-Path $dir 'comment.json') $true
Check 'comment add' ($r.code -eq 0)
$r = G '/api/comments?spotId=10&size=5'
Check 'comment visible' ($r.code -eq 0 -and @($r.data.list | Where-Object { $_.content -match '鸟巢' }).Count -ge 1)
WJson 'short.json' '{"spotId":10,"rating":5,"content":"短"}'
$short = Join-Path $dir 'short.json'
$r = P '/api/comments' $short $true
Check 'short comment rejected' ($null -ne $r -and $r.code -ne 0)

# ---------- 虚拟游览 ----------
$r = P '/api/tour/start' (Join-Path $dir 'start.json') $false
Check 'tour start' ($r.code -eq 0 -and $r.data.sessionId)
$sid = $r.data.sessionId
$spotIds = @($route.spots | ForEach-Object { $_.spotId })
$visitedOk = 0
foreach ($s in $spotIds) {
  WJson "visit_$s.json" "{`"sessionId`":`"$sid`",`"routeId`":1,`"spotId`":$s}"
  $rr = P '/api/tour/visit' (Join-Path $dir "visit_$s.json") $false
  if ($rr.code -eq 0) { $visitedOk++ }
}
Check "tour visit all steps ($($spotIds.Count))" ($visitedOk -eq @($spotIds).Count) "$visitedOk/$(@($spotIds).Count)"
$r = G "/api/tour/progress?sessionId=$sid&routeId=1"
Check 'tour progress finished' ($r.code -eq 0 -and $r.data.finished -eq $true -and $r.data.percent -eq 100)
WJson 'visit_dup.json' "{`"sessionId`":`"$sid`",`"routeId`":1,`"spotId`":$($spotIds[0])}"
$r = P '/api/tour/visit' (Join-Path $dir 'visit_dup.json') $false
Check 'duplicate visit graceful' ($null -ne $r -and $r.code -eq 0)
$r = G "/api/tour/records?sessionId=$sid"
Check 'tour records with province' ($r.code -eq 0 -and @($r.data).Count -ge @($spotIds).Count -and $null -ne $r.data[0].province)
$r = G "/api/tour/footprint?sessionId=$sid"
Check 'footprint aggregate' ($r.code -eq 0 -and $r.data.spotCount -ge @($spotIds).Count -and $r.data.provinceCount -ge 1)
Check 'footprint badges' (@($r.data.badges).Count -ge 1 -and ($r.data.badges -join ',') -match '初出茅庐')
$r = G "/api/tour/footprint?sessionId=brand-new-nonexistent-$(Get-Random)"
Check 'footprint empty graceful' ($r.code -eq 0 -and $r.data.provinceCount -eq 0 -and @($r.data.badges).Count -eq 0)

# ---------- 异常资源 ----------
$r = G '/api/spots/99999'
Check 'missing spot graceful' ($null -ne $r -and $r.code -ne 0)
$r = G '/api/routes/99999'
Check 'missing route graceful' ($null -ne $r -and $r.code -ne 0)

# ---------- 未登录保护 ----------
$raw = & curl.exe -s -b $jar2 -c $jar2 -H 'Content-Type: application/json; charset=utf-8' --data-binary "@$(Join-Path $dir 'favorite.json')" "$base/api/favorites"
$r = J $raw
Check 'unauth favorite blocked' ($null -ne $r -and $r.code -ne 0)

# ---------- 退出 ----------
# 恢复 demo 昵称, 保证冒烟可重复执行
WJson 'reset.json' '{"nickname":"小明同学","preferenceTags":"摄影,亲子,温泉"}'
$null = P '/api/auth/profile' (Join-Path $dir 'reset.json') $true
$r = P '/api/auth/logout' (Join-Path $dir 'login.json') $true
Check 'logout' ($r.code -eq 0)
$r = G '/api/auth/me'
Check 'me after logout' ($null -ne $r -and $r.code -eq 0 -and $null -eq $r.data)

Write-Host ''
Write-Host "==== RESULT: PASS=$script:pass FAIL=$script:fail ===="
if ($script:fail -gt 0) { Write-Host ('FAILED: ' + ($script:fails -join ', ')); exit 1 }
