# -*- coding: utf-8 -*-
"""云游中国 · 答辩 PPT 生成脚本 58 页版（python-pptx）
运行: python make_ppt.py → C:\\Users\\mhw78\\Desktop\\云游中国-答辩PPT-58页.pptx
截图: 读取桌面 01~20 运行截图 + arch1~3 架构截图，缺失则绘制占位卡
"""
import os
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.oxml.ns import qn

GREEN_D = RGBColor(0x0B, 0x7A, 0x54)
GREEN   = RGBColor(0x0E, 0x9F, 0x6E)
GREEN_L = RGBColor(0xE8, 0xF7, 0xF0)
INK     = RGBColor(0x1F, 0x29, 0x37)
GREY    = RGBColor(0x6B, 0x72, 0x80)
WHITE   = RGBColor(0xFF, 0xFF, 0xFF)
GOLD    = RGBColor(0xF5, 0x9E, 0x0B)
CODE_BG = RGBColor(0x1E, 0x25, 0x30)
CODE_TX = RGBColor(0xD8, 0xE4, 0xDA)
CODE_CM = RGBColor(0x7E, 0xC9, 0xA3)
LINE_C  = RGBColor(0xD7, 0xDE, 0xE6)
WARN_C  = RGBColor(0xB4, 0x59, 0x2C)
BLUE_D  = RGBColor(0x25, 0x63, 0xEB)

SW, SH = Inches(13.333), Inches(7.5)
DESK = r"C:\Users\mhw78\Desktop"

def set_font(run, size=18, bold=False, color=INK, name="微软雅黑"):
    f = run.font
    f.size = Pt(size); f.bold = bold; f.color.rgb = color; f.name = name
    rPr = run._r.get_or_add_rPr()
    for tag in ("a:ea", "a:cs"):
        e = rPr.find(qn(tag))
        if e is None:
            e = rPr.makeelement(qn(tag), {}); rPr.append(e)
        e.set("typeface", name)

def add_slide(prs):
    return prs.slides.add_slide(prs.slide_layouts[6])

def rect(slide, x, y, w, h, fill, line=None, round_=False):
    shp = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE if round_ else MSO_SHAPE.RECTANGLE, x, y, w, h)
    shp.fill.solid(); shp.fill.fore_color.rgb = fill
    if line: shp.line.color.rgb = line; shp.line.width = Pt(1.1)
    else: shp.line.fill.background()
    shp.shadow.inherit = False
    return shp

def text(slide, x, y, w, h, lines, align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, wrap=True):
    """lines: [(txt, size, bold, color, space_after_pt)]"""
    tb = slide.shapes.add_textbox(x, y, w, h)
    tf = tb.text_frame; tf.word_wrap = wrap; tf.vertical_anchor = anchor
    first = True
    for (t, s, b, c, sa) in lines:
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.alignment = align; p.space_after = Pt(sa)
        r = p.add_run(); r.text = t; set_font(r, s, b, c)
    return tb

def header(slide, num, title, sub=""):
    rect(slide, 0, 0, SW, Inches(1.08), GREEN_D)
    rect(slide, 0, Inches(1.08), SW, Pt(3), GOLD)
    text(slide, Inches(0.55), Inches(0.14), Inches(1.15), Inches(0.8),
         [(num, 28, True, WHITE, 0)], anchor=MSO_ANCHOR.MIDDLE)
    text(slide, Inches(1.5), Inches(0.10), Inches(10.9), Inches(0.6), [(title, 23, True, WHITE, 0)])
    if sub:
        text(slide, Inches(1.53), Inches(0.62), Inches(10.9), Inches(0.38), [(sub, 11.5, False, RGBColor(0xCF, 0xF0, 0xE2), 0)])
    text(slide, SW - Inches(2.45), SH - Inches(0.4), Inches(2.15), Inches(0.3),
         [("云游中国 · SSM 全栈实践", 9.5, False, GREY, 0)], align=PP_ALIGN.RIGHT)

def card(slide, x, y, w, h, title, body, tsize=15, bsize=12, tcolor=GREEN_D):
    rect(slide, x, y, w, h, GREEN_L, round_=True)
    rect(slide, x, y, Pt(4), h, GREEN)
    lines = [(title, tsize, True, tcolor, 4)]
    for seg in body.split("\n"):
        lines.append((seg, bsize, False, INK, 1))
    text(slide, x + Inches(0.22), y + Inches(0.10), w - Inches(0.4), h - Inches(0.2), lines)

def code_block(slide, x, y, w, h, lines, size=11.5):
    """lines: [(txt, is_comment)]"""
    rect(slide, x, y, w, h, CODE_BG, round_=True)
    tf_lines = []
    for t, cm in lines:
        tf_lines.append((t, size, False, CODE_CM if cm else CODE_TX, 1))
    text(slide, x + Inches(0.25), y + Inches(0.14), w - Inches(0.5), h - Inches(0.28), tf_lines)

def stat_block(slide, x, y, w, h, num, label, num_size=30):
    rect(slide, x, y, w, h, WHITE, line=LINE_C, round_=True)
    text(slide, x, y + Inches(0.08), w, Inches(0.6), [(num, num_size, True, GREEN_D, 0)], align=PP_ALIGN.CENTER)
    text(slide, x, y + h - Inches(0.44), w, Inches(0.34), [(label, 11, False, GREY, 0)], align=PP_ALIGN.CENTER)

def shot(slide, fname, title, desc, x=Inches(0.7), y=Inches(1.45), w=Inches(7.6)):
    """右侧大截图 + 左侧说明；缺图则占位"""
    path = os.path.join(DESK, fname)
    iw = w; ih = Emu(int(iw * 9 / 16))
    if os.path.exists(path):
        rect(slide, x - Pt(2), y - Pt(2), iw + Pt(4), ih + Pt(4), GREEN_D)
        slide.shapes.add_picture(path, x, y, width=iw)
    else:
        rect(slide, x, y, iw, ih, RGBColor(0xF0, 0xF4, 0xF2), line=GREEN, round_=True)
        text(slide, x, y + ih / 2 - Inches(0.4), iw, Inches(0.8),
             [("🖥️ 运行截图待补", 20, True, GREEN_D, 3),
              ("启动 Tomcat 后重新运行本脚本自动填入", 12, False, GREY, 0)], align=PP_ALIGN.CENTER)
    lx = x + iw + Inches(0.35)
    lw = SW - lx - Inches(0.55)
    lines = [(title, 18, True, GREEN_D, 6)]
    for seg in desc.split("\n"):
        lines.append((seg, 12.5, False, INK, 4))
    text(slide, lx, y + Inches(0.1), lw, ih, lines)

def divider(prs_, cn, en, note=""):
    slide = prs_.slides.add_slide(prs_.slide_layouts[6])
    rect(slide, 0, 0, SW, SH, GREEN_D)
    rect(slide, 0, Inches(4.42), SW, Pt(4), GOLD)
    text(slide, Inches(1.1), Inches(2.6), Inches(11.1), Inches(1.6),
         [(cn, 44, True, WHITE, 8), (en, 16, False, RGBColor(0xCF, 0xF0, 0xE2), 0)])
    if note:
        text(slide, Inches(1.1), Inches(4.75), Inches(11.1), Inches(0.8), [(note, 14, False, RGBColor(0xCF, 0xF0, 0xE2), 0)])

def table_rows(slide, rows, x=Inches(0.8), y=Inches(1.4), w=Inches(11.7), rh=Inches(0.5), fs=12):
    yy = y
    for i, (a, b) in enumerate(rows):
        rect(slide, x, yy, Inches(3.0), rh, GREEN_L if i == 0 else WHITE, line=LINE_C)
        rect(slide, x + Inches(3.0), yy, w - Inches(3.0), rh, GREEN_L if i == 0 else WHITE, line=LINE_C)
        text(slide, x + Inches(0.14), yy + Inches(0.05), Inches(2.8), rh - Inches(0.06),
             [(a, fs, i == 0, GREEN_D if i == 0 else INK, 0)])
        text(slide, x + Inches(3.14), yy + Inches(0.05), w - Inches(3.3), rh - Inches(0.06),
             [(b, fs, i == 0, GREEN_D if i == 0 else INK, 0)])
        yy += rh
    return yy

prs = Presentation()
prs.slide_width, prs.slide_height = SW, SH

# ================= P1 封面 =================
s = add_slide(prs)
rect(s, 0, 0, SW, SH, GREEN_D)
rect(s, 0, SH - Inches(1.75), SW, Inches(1.75), RGBColor(0x06, 0x3D, 0x2B))
rect(s, 0, Inches(2.58), SW, Pt(4), GOLD)
text(s, Inches(1.0), Inches(1.05), Inches(11.3), Inches(0.6),
     [("Java Web 课程项目 · SSM 框架综合实践", 15, False, RGBColor(0xCF, 0xF0, 0xE2), 0)])
text(s, Inches(1.0), Inches(1.5), Inches(11.3), Inches(1.0), [("云游中国", 50, True, WHITE, 0)])
text(s, Inches(1.0), Inches(2.82), Inches(11.3), Inches(0.6),
     [("全国景点智能导览推荐系统 —— Spring + Spring MVC + MyBatis 全栈实践", 18, False, WHITE, 0)])
chips = ["Spring 7.0.9", "MyBatis 3.5.19", "MySQL 8", "Tomcat 11", "AI 三引擎+兜底", "VR 360° 全景", "双通道支付"]
cx = Inches(1.0)
for c in chips:
    w = Inches(0.32 + 0.155 * len(c))
    rect(s, cx, Inches(3.68), w, Inches(0.42), GREEN, round_=True)
    text(s, cx, Inches(3.72), w, Inches(0.34), [(c, 11, True, WHITE, 0)], align=PP_ALIGN.CENTER)
    cx += w + Inches(0.16)
nums = [("115", "Java 类"), ("50+", "REST 接口"), ("15", "JSP 路由"), ("8", "核心创新点"), ("231", "真实景点")]
sx = Inches(1.0)
for n, lb in nums:
    stat_block(s, sx, Inches(4.6), Inches(2.05), Inches(1.2), n, lb)
    sx += Inches(2.25)
text(s, Inches(1.0), Inches(6.2), Inches(11.3), Inches(0.8),
     [("答辩人：___________        学号：___________        指导教师：___________        2026 年 10 月", 13.5, False, RGBColor(0xCF, 0xF0, 0xE2), 0)])

# ================= P2 目录 =================
s = add_slide(prs)
header(s, "02", "汇报提纲（62 页）", "CONTENTS · 七个篇章")
toc = [
    ("第一篇 · 项目概述", "P3-5    背景 / 定位 / 核心指标"),
    ("第二篇 · 系统架构", "P6-10   技术选型 / 五层架构 / 请求链路 / IoC / 配置"),
    ("第三篇 · 数据库设计", "P11-17  14 表总览 / ER 图 / 核心表详解 / 动态 SQL"),
    ("第四篇 · 工程结构深度解析", "P18-27  目录树 / 后端包 / 前端资源 + IDEA 架构截图 ×7"),
    ("第五篇 · 功能演示（运行截图 20 张）", "P28-48  SPA / JSP / VR / 支付 / 管理端全流程实景"),
    ("第六篇 · 技术深度解析", "P49-60  AI 引擎 / RAG / VR / 事务 / 支付 / AOP"),
    ("第七篇 · 总结", "P61-62  统计难点 / 致谢"),
]
y = Inches(1.5)
for i, (t, d) in enumerate(toc):
    rect(s, Inches(0.9), y, Inches(11.5), Inches(0.72), GREEN_L if i % 2 == 0 else WHITE, round_=True)
    text(s, Inches(1.25), y + Inches(0.16), Inches(5.8), Inches(0.5), [(t, 16, True, GREEN_D, 0)])
    text(s, Inches(7.1), y + Inches(0.19), Inches(5.1), Inches(0.5), [(d, 11.5, False, GREY, 0)])
    y += Inches(0.82)

# ================= P3 背景 =================
s = add_slide(prs)
header(s, "03", "项目背景 · 三大痛点", "从真实需求出发，而非为做而做")
card(s, Inches(0.7), Inches(1.5), Inches(3.9), Inches(2.5), "痛点一 · 信息失真",
     "攻略“照骗”泛滥\n图片与实景严重不符\n出行决策成本高", 15, 13)
card(s, Inches(4.75), Inches(1.5), Inches(3.9), Inches(2.5), "痛点二 · 规划低效",
     "景点信息分散多平台\n行程组合全靠手工拼凑\n耗时费力易遗漏", 15, 13)
card(s, Inches(8.8), Inches(1.5), Inches(3.9), Inches(2.5), "痛点三 · 无法预览",
     "出行前无法直观感受\n景区真实环境\n期望与体验落差大", 15, 13)
rect(s, Inches(0.7), Inches(4.3), Inches(12.0), Inches(1.1), GREEN_L, round_=True)
text(s, Inches(1.0), Inches(4.45), Inches(11.5), Inches(0.85),
     [("项目定位：数据真实 · 体验沉浸 · 决策智能 的一站式旅游导览平台", 18, True, GREEN_D, 3),
      ("覆盖 31 省 231 个真实景点 —— 数据、AI 讲解、VR 全景全部真实可溯", 13, False, INK, 0)])
text(s, Inches(0.7), Inches(5.75), Inches(12.0), Inches(1.3),
     [("四条设计原则", 15, True, GREEN_D, 5),
      ("① 数据真实 —— RAG 约束 AI 不编造，景点/评论全部真实数据", 12.5, False, INK, 2),
      ("② 架构规范 —— SSM 五层 + AOP + 声明式事务，注解驱动零 XML", 12.5, False, INK, 2),
      ("③ 体验沉浸 —— 纯 CSS 3D 真 360° 全景，零 WebGL 依赖   ④ 链路完整 —— 购票→支付→出票→退款闭环", 12.5, False, INK, 0)])

# ================= P4 定位与目标用户 =================
s = add_slide(prs)
header(s, "04", "项目定位与用户角色", "三类角色 · 完整业务闭环")
card(s, Inches(0.75), Inches(1.5), Inches(3.85), Inches(2.7), "👤 游客用户",
     "浏览 / 搜索 / 推荐景点\nAI 问答与行程规划\nVR 云游打卡\n购票支付 / 管理订单 / 评价收藏", 15, 12.5)
card(s, Inches(4.75), Inches(1.5), Inches(3.85), Inches(2.7), "🛡️ 管理员",
     "景点上下架与信息维护\n订单查询与处理\n用户管理\n（写操作全部被 AOP 审计）", 15, 12.5)
card(s, Inches(8.75), Inches(1.5), Inches(3.85), Inches(2.7), "🤖 AI 引擎群",
     "景点讲解 / 智能问答\n行程路线生成\n一句话游记\n四引擎故障转移保障可用", 15, 12.5)
text(s, Inches(0.75), Inches(4.5), Inches(11.9), Inches(2.4),
     [("业务闭环", 16, True, GREEN_D, 6),
      ("发现（推荐/地图/盲盒）→ 了解（详情/AI 讲解/VR 实景）→ 决策（行程规划）→ 交易（下单/支付）→ 凭证（二维码票根）", 13, False, INK, 3),
      ("→ 售后（退款/删除）→ 反哺（打卡数据驱动热度榜，热度反哺推荐）—— 数据形成正向循环", 13, True, GREEN_D, 0)])

# ================= P5 核心指标 =================
s = add_slide(prs)
header(s, "05", "项目核心指标", "先看体量，再看深度")
row1 = [("115", "Java 类"), ("7,128", "Java 代码行"), ("50+", "REST 接口"), ("15", "JSP 路由")]
row2 = [("14", "数据库表"), ("231", "真实景点"), ("335", "用户评论"), ("16", "主题路线")]
row3 = [("31", "覆盖省份"), ("8", "核心创新点"), ("4", "AI 引擎(含兜底)"), ("30", "VR 全景贴图")]
y = Inches(1.55)
for row in (row1, row2, row3):
    x = Inches(0.85)
    for n, lb in row:
        stat_block(s, x, y, Inches(2.8), Inches(1.5), n, lb, num_size=32)
        x += Inches(3.0)
    y += Inches(1.72)
text(s, Inches(0.85), Inches(6.75), Inches(11.7), Inches(0.55),
     [("JS 核心 2,104 行 · 单元测试 6 个 · MyBatis Mapper 12 个 —— 每一个数字都对应真实文件，可现场清点", 13, True, GREEN_D, 0)], align=PP_ALIGN.CENTER)

# ================= P6 技术选型 =================
s = add_slide(prs)
header(s, "06", "第二篇 · 技术选型", "每一项都有明确理由，拒绝为用而用")
yy = table_rows(s, [
    ("技术 / 版本", "选型理由"),
    ("Spring 7.0.9", "IoC/DI + AOP + 声明式事务；7.x 基于 Jakarta EE，兼容 JDK 17/25 与 Tomcat 11"),
    ("Spring MVC 7.0.9", "@RestController(JSON) 与 @Controller(JSP) 双输出，支撑双视图架构"),
    ("MyBatis 3.5.19", "注解 SQL + <script> 动态 SQL，防注入白名单与手写物理分页可控性强"),
    ("MySQL 8.x", "utf8mb4 + InnoDB，事务引擎支撑订单状态机"),
    ("Tomcat 11", "Jakarta Servlet 6 容器，与 Spring 7 的 jakarta.* 命名空间完全匹配"),
    ("原生 JS SPA", "零框架 History 路由，约 2100 行核心代码，体现前端功底"),
    ("Leaflet 1.9.4", "本地化部署不依赖 CDN，高德瓦片源，校园网可用"),
    ("AI 引擎 ×3+兜底", "DeepSeek / 通义 qwen-turbo / 豆包 + 本地规则，分级超时故障转移"),
])
text(s, Inches(0.8), yy + Inches(0.2), Inches(11.7), Inches(0.6),
     [("关键决策：JDK 25 与 javax.* 传统 SSM 不兼容 → 全面升级 Jakarta EE 代际（Spring 7 + Tomcat 11），一次到位", 12.5, True, GREEN_D, 0)])

# ================= P7 五层架构 =================
s = add_slide(prs)
header(s, "07", "SSM 五层架构图", "注解驱动零 XML · DispatcherServlet 替代 web.xml")
layers = [
    ("视图层 View", "SPA（原生 JS + History 路由） · 14 个 JSP（JSTL/EL） · admin.html · VR 环景舱", GREEN),
    ("控制层 Controller", "17 个 Controller · @RestController(JSON) / @Controller(JSP) 双输出", GREEN_D),
    ("业务层 Service", "30 个类（接口+实现）· @Service · @Transactional · 状态机/故障转移", GREEN_D),
    ("持久层 DAO", "@Repository DAO 实现注入 MyBatis Mapper · 隔离业务与 SQL", GREEN_D),
    ("数据层 MySQL", "14 张表 · InnoDB · utf8mb4 · 231 景点 / 335 评论 / 订单 / 审计", GREEN),
]
y = Inches(1.42)
for i, (t, d, c) in enumerate(layers):
    rect(s, Inches(0.9), y, Inches(11.5), Inches(0.74), GREEN_L, line=c, round_=True)
    rect(s, Inches(0.9), y, Inches(2.95), Inches(0.74), c, round_=True)
    text(s, Inches(0.9), y + Inches(0.17), Inches(2.95), Inches(0.5), [(t, 14, True, WHITE, 0)], align=PP_ALIGN.CENTER)
    text(s, Inches(4.05), y + Inches(0.2), Inches(8.2), Inches(0.5), [(d, 12, False, INK, 0)])
    if i < 4:
        text(s, Inches(6.45), y + Inches(0.68), Inches(0.8), Inches(0.3), [("▼", 12, True, GREEN, 0)], align=PP_ALIGN.CENTER)
    y += Inches(1.0)
text(s, Inches(0.9), Inches(6.5), Inches(11.5), Inches(0.8),
     [("横切能力（贯穿五层）：AOP 审计 @Aspect 零侵入 · 声明式事务 @Transactional · 启动自举（幂等建表+种子数据）", 12.5, False, GREY, 0)])

# ================= P8 请求链路 =================
s = add_slide(prs)
header(s, "08", "一次请求的完整旅程", "GET /api/spots?province=四川&sort=rating 为例")
code_block(s, Inches(0.8), Inches(1.45), Inches(11.7), Inches(3.6), [
    ("浏览器  GET /api/spots?province=四川&sort=rating", False),
    ("  ↓ DispatcherServlet（WebAppInitializer 注册，替代 web.xml）", True),
    ("  ↓ SpotController.list()          @RestController + @RequestParam 参数绑定", False),
    ("  ↓ SpotService.listSpots()        @Service —— sort 白名单映射: rating→rating DESC", False),
    ("  ↓ SpotDaoImpl.find()             @Repository —— 调用 Mapper 接口", False),
    ("  ↓ SpotMapper 动态 SQL            <script> + #{} 预编译 + LIMIT 物理分页", False),
    ("  ↓ MySQL 命中索引 province_idx    返回行集", False),
    ("  ↑ 逐层返回 → Jackson 序列化 → {code:0, data:{list:[…], total:87}}", False),
    ("  ↑ 前端 fetch → 渲染景点卡片网格 + 分页条", False),
], size=13)
text(s, Inches(0.8), Inches(5.35), Inches(11.7), Inches(1.7),
     [("分层价值", 15, True, GREEN_D, 5),
      ("① 每层职责单一：Controller 只做参数校验与转发，业务规则全部在 Service，SQL 细节隔离在 DAO/Mapper", 12.5, False, INK, 2),
      ("② 可替换性：换前端（SPA↔JSP）不动 Service；换存储不动业务；AOP 横切能力即插即用", 12.5, False, INK, 0)])

# ================= P9 IoC 解析 =================
s = add_slide(prs)
header(s, "09", "IoC 容器与依赖注入解析", "AppConfig + WebAppInitializer —— 注解驱动零 XML")
code_block(s, Inches(0.75), Inches(1.45), Inches(5.85), Inches(3.3), [
    ("@Configuration", False),
    ("@ComponentScan(\"cn.edu.qut\")", False),
    ("@PropertySource({...app/ai/alipay}) ", False),
    ("public class AppConfig {", False),
    ("  // 数据源 + 事务管理器 + MyBatis", True),
    ("  // databaseBootstrap: 启动自举", True),
    ("  //   CREATE TABLE IF NOT EXISTS ×12", True),
    ("  //   空库自动灌入 231 景点种子数据", True),
    ("}", False),
], size=12)
code_block(s, Inches(6.75), Inches(1.45), Inches(5.85), Inches(3.3), [
    ("public class WebAppInitializer", False),
    ("    extends AbstractAnnotationConfig-", False),
    ("        DispatcherServletInitializer {", False),
    ("  getRootConfigClasses → AppConfig", False),
    ("  // DispatcherServlet 映射 \"/\"", True),
    ("  // addResourceHandlers 静态资源", True),
    ("  // 全局异常处理器兜底", True),
    ("}", False),
], size=12)
text(s, Inches(0.75), Inches(5.05), Inches(11.9), Inches(2.0),
     [("注入方式与层次约束（严格执行）", 15, True, GREEN_D, 5),
      ("Controller 只注入 Service 接口（不注入 Impl）→ ServiceImpl 只注入 DAO 接口 → DAOImpl 只注入 Mapper", 12.5, False, INK, 2),
      ("效果：面向接口编程，任一层实现可整体替换；构造器注入保证依赖不可变、容器启动即完成装配检查", 12.5, True, GREEN_D, 0)])

# ================= P10 配置文件解析 =================
s = add_slide(prs)
header(s, "10", "三大配置文件解析", "@PropertySource 装配 · 敏感信息与代码分离")
code_block(s, Inches(0.75), Inches(1.45), Inches(3.8), Inches(4.1), [
    ("# app.properties", True),
    ("jdbc.url=jdbc:mysql://", False),
    ("  localhost:3306/scenic_db", False),
    ("  ?characterEncoding=UTF-8", False),
    ("jdbc.username=root", False),
    ("jdbc.password=***", False),
    ("# 注: UTF-8 而非 utf8mb4", True),
    ("# 避免连接初始化崩溃", True),
], size=11)
code_block(s, Inches(4.72), Inches(1.45), Inches(3.8), Inches(4.1), [
    ("# ai.properties", True),
    ("ai.providers=", False),
    ("  deepseek,qwen,doubao", False),
    ("ai.deepseek.model=", False),
    ("  deepseek-chat", False),
    ("ai.qwen.model=qwen-turbo", False),
    ("ai.doubao.model=ep-***", False),
    ("# 各引擎独立 key/timeout", True),
    ("# 10s / 8s / 15s 分级", True),
], size=11)
code_block(s, Inches(8.7), Inches(1.45), Inches(3.85), Inches(4.1), [
    ("# alipay.properties", True),
    ("alipay.appId=9021***", False),
    ("alipay.gatewayUrl=", False),
    ("  openapi-sandbox…/gateway.do", False),
    ("alipay.appPrivateKey=***", False),
    ("alipay.alipayPublicKey=***", False),
    ("# 私钥 PKCS#8 本地保管", True),
    ("# 不入版本库", True),
], size=11)
text(s, Inches(0.75), Inches(5.85), Inches(11.9), Inches(1.2),
     [("工程意义：换环境只改配置不改代码；密钥泄漏风险隔离；AI 引擎顺序/超时调整零重启成本（重启加载即可）", 12.5, False, INK, 0)])

# ================= P11 数据库总览 =================
s = add_slide(prs)
header(s, "11", "第三篇 · 数据库总览", "MySQL 8 · 14 张表 · InnoDB + utf8mb4 · 幂等建表")
tbls = [
    ("scenic_spot", "景点主表 231 条：名称/省份/城市/经纬度/评分/价格/简介/图片/标签"),
    ("category / province", "景点分类 + 省份维度表 31 省（七大地理分区索引）"),
    ("sys_user / spot_comment / favorite", "用户域三件套：用户 · 评论 335 条 · 收藏（双外键唯一约束）"),
    ("ticket_order", "票务订单：order_no 唯一键 / pay_status 状态机 / pay_method / 金额"),
    ("route / route_spot", "16 条主题路线 + 路线-景点关联表（含分步 AI 讲解词 narration）"),
    ("tour_record / vr_view_log", "虚拟游览打卡 + VR 环景舱记录 → 热度榜数据源"),
    ("travel_preference / travel_plan", "智能规划：偏好问卷 → AI 生成路线落库（provider 引擎标识）"),
    ("operation_log", "AOP 审计日志：管理员/操作类型/目标对象/参数摘要/时间（@Aspect 自动写入）"),
]
y = Inches(1.45)
for i, (t, d) in enumerate(tbls):
    rect(s, Inches(0.85), y, Inches(11.6), Inches(0.6), GREEN_L if i % 2 == 0 else WHITE, round_=True)
    text(s, Inches(1.15), y + Inches(0.13), Inches(3.5), Inches(0.44), [(t, 13, True, GREEN_D, 0)])
    text(s, Inches(4.8), y + Inches(0.145), Inches(7.5), Inches(0.44), [(d, 11.5, False, INK, 0)])
    y += Inches(0.68)
text(s, Inches(0.85), y + Inches(0.1), Inches(11.6), Inches(0.5),
     [("全部 CREATE TABLE IF NOT EXISTS —— 启动自举校验建表，空库自动灌种子数据，一次启动即可运行", 12.5, True, GREEN_D, 0)])

# ================= P12 ER 图 =================
s = add_slide(prs)
header(s, "12", "ER 实体关系图", "8 个核心实体 · 围绕「用户—景点」双中心展开")

def er_box(x, y, w, h, name, fill=GREEN_L, line=GREEN_D):
    rect(s, x, y, w, h, fill, line=line, round_=True)
    text(s, x, y + h / 2 - Inches(0.16), w, Inches(0.34), [(name, 12.5, True, GREEN_D, 0)], align=PP_ALIGN.CENTER)

def er_link(x1, y1, x2, y2, label="1:N"):
    conn = s.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, x1, y1, x2, y2)
    conn.line.color.rgb = GREEN; conn.line.width = Pt(1.4)
    text(s, (x1 + x2) / 2 - Inches(0.4), (y1 + y2) / 2 - Inches(0.22), Inches(0.8), Inches(0.3),
         [(label, 9.5, True, GREY, 0)], align=PP_ALIGN.CENTER)

er_box(Inches(5.3), Inches(1.5), Inches(2.7), Inches(0.62), "user 用户")           # 中心左上
er_box(Inches(5.3), Inches(4.4), Inches(2.7), Inches(0.62), "scenic_spot 景点")    # 中心
er_box(Inches(0.9), Inches(1.5), Inches(2.5), Inches(0.62), "review 评论")
er_box(Inches(0.9), Inches(2.9), Inches(2.5), Inches(0.62), "favorite 收藏")
er_box(Inches(0.9), Inches(4.4), Inches(2.5), Inches(0.62), "ticket_order 订单")
er_box(Inches(0.9), Inches(5.8), Inches(2.5), Inches(0.62), "travel_plan 规划")
er_box(Inches(9.9), Inches(2.9), Inches(2.5), Inches(0.62), "vr_view_log 打卡")
er_box(Inches(9.9), Inches(4.4), Inches(2.5), Inches(0.62), "operation_log 审计")
er_box(Inches(9.9), Inches(5.8), Inches(2.5), Inches(0.62), "route 路线")
er_link(Inches(3.4), Inches(1.81), Inches(5.3), Inches(1.75), "1:N")
er_link(Inches(3.4), Inches(3.21), Inches(5.3), Inches(1.9), "1:N")
er_link(Inches(3.4), Inches(4.71), Inches(5.3), Inches(4.65), "1:N")
er_link(Inches(3.4), Inches(6.11), Inches(5.3), Inches(4.85), "1:N")
er_link(Inches(8.0), Inches(1.9), Inches(9.9), Inches(3.2), "1:N")
er_link(Inches(8.0), Inches(4.71), Inches(9.9), Inches(4.71), "1:N")
er_link(Inches(6.65), Inches(2.12), Inches(6.65), Inches(4.4), "N:M")
text(s, Inches(9.0), Inches(6.6), Inches(3.6), Inches(0.7),
     [("operation_log 由 AOP 切面写入，\n与业务实体解耦", 10.5, False, GREY, 0)])
text(s, Inches(0.9), Inches(6.6), Inches(7.6), Inches(0.8),
     [("关系要点：user 与 scenic_spot 为双中心；订单/评论/收藏/打卡/规划均挂双外键；", 11.5, False, INK, 2),
      ("热度榜 = vr_view_log 按 spot_id GROUP BY 聚合，不加冗余计数字段，保证数据一致", 11.5, False, INK, 0)])

# ================= P13 表详解1 =================
s = add_slide(prs)
header(s, "13", "核心表详解 ① 用户域", "scenic_spot · sys_user · spot_comment · favorite")
code_block(s, Inches(0.75), Inches(1.45), Inches(6.0), Inches(4.0), [
    ("CREATE TABLE IF NOT EXISTS scenic_spot (", False),
    ("  id BIGINT PRIMARY KEY AUTO_INCREMENT,", False),
    ("  name VARCHAR(80) NOT NULL,", False),
    ("  province VARCHAR(30),  city VARCHAR(30),", False),
    ("  lng DECIMAL(10,6), lat DECIMAL(10,6),   -- 地图", True),
    ("  rating DECIMAL(2,1), price DECIMAL(8,2),", False),
    ("  summary VARCHAR(500), image_url VARCHAR(200),", False),
    ("  tags VARCHAR(200), status TINYINT DEFAULT 1,", False),
    ("  INDEX idx_province (province),", True),
    ("  INDEX idx_rating (rating)", True),
    (");", False),
], size=11.5)
text(s, Inches(7.0), Inches(1.5), Inches(5.6), Inches(5.2),
     [("设计要点", 15, True, GREEN_D, 6),
      ("• 经纬度独立两列（DECIMAL 10,6），支撑 Leaflet 地图全量标注", 12, False, INK, 4),
      ("• tags 逗号分隔存多标签，偏好推荐 LIKE 匹配 + Service 加权", 12, False, INK, 4),
      ("• status 逻辑删除位：管理端“删除”= 置 0，数据可溯", 12, False, INK, 4),
      ("• province/rating 高频查询列建索引，排行榜毫秒级", 12, False, INK, 4),
      ("• review 表含 spot_id + user_id 双外键 + content + rating，335 条真实评论支撑评分可信", 12, False, INK, 0)])

# ================= P14 表详解2 订单 =================
s = add_slide(prs)
header(s, "14", "核心表详解 ② ticket_order 订单", "状态机字段设计 —— 一切以服务端为准")
code_block(s, Inches(0.75), Inches(1.45), Inches(6.0), Inches(4.0), [
    ("CREATE TABLE IF NOT EXISTS ticket_order (", False),
    ("  id BIGINT PRIMARY KEY AUTO_INCREMENT,", False),
    ("  order_no VARCHAR(40) NOT NULL UNIQUE,", True),
    ("  -- ↑ 业务单号 = 幂等键 + 对账键", True),
    ("  user_id BIGINT NOT NULL,", False),
    ("  spot_id BIGINT NOT NULL,", False),
    ("  amount DECIMAL(10,2) NOT NULL,", True),
    ("  -- ↑ 服务端计算，不信前端", True),
    ("  status VARCHAR(12) DEFAULT 'PENDING',", False),
    ("  pay_method VARCHAR(12),   -- ALIPAY/…", False),
    ("  create_time DATETIME, pay_time DATETIME", False),
    (");", False),
], size=11.5)
text(s, Inches(7.0), Inches(1.5), Inches(5.6), Inches(5.4),
     [("状态机流转规则", 15, True, GREEN_D, 6),
      ("PENDING ──支付──▶ PAID ──退款──▶ CANCELED", 13, True, GREEN_D, 6),
      ("• 流转 = CAS 条件 UPDATE（WHERE status=旧值），并发重复回调只生效一次", 12, False, INK, 4),
      ("• 非法流转（CANCELED→PAID 等）被状态条件天然拒绝，无需额外 if", 12, False, INK, 4),
      ("• order_no 全局唯一：下单幂等 + 支付对账 + 主动查询补单，三处复用", 12, False, INK, 4),
      ("• 金额服务端算：前端只传“买什么/几张”，报价查库计算，杜绝篡改", 12, False, INK, 0)])

# ================= P15 表详解3 =================
s = add_slide(prs)
header(s, "15", "核心表详解 ③ 数据闭环三表", "vr_view_log · operation_log · travel_plan")
card(s, Inches(0.75), Inches(1.5), Inches(3.85), Inches(2.6), "vr_view_log 打卡",
     "user_id / spot_id / duration / view_time\n\nVR 内点打卡即插一条；\n首页热度榜 = GROUP BY spot_id\nCOUNT 聚合，实时真实", 14.5, 12)
card(s, Inches(4.75), Inches(1.5), Inches(3.85), Inches(2.6), "operation_log 审计",
     "admin_id / action / target / params / time\n\n由 @Aspect 环绕通知自动写入；\n管理端任何增删改全留痕，\nController 零改动", 14.5, 12)
card(s, Inches(8.75), Inches(1.5), Inches(3.85), Inches(2.6), "travel_plan 规划",
     "preference JSON / plan JSON / engine\n\nAI 生成路线整体落库；\n相同偏好组合二次请求\n毫秒级复用", 14.5, 12)
rect(s, Inches(0.75), Inches(4.4), Inches(11.9), Inches(2.35), GREEN_L, round_=True)
text(s, Inches(1.05), Inches(4.58), Inches(11.3), Inches(2.0),
     [("三张表共同的设计哲学", 15, True, GREEN_D, 5),
      ("① 只记录事实（打卡了/操作了/生成了），统计与排行全部查询时聚合计算 —— 避免冗余字段的数据不一致", 12.5, False, INK, 3),
      ("② 全部含时间戳，可做趋势分析（如近 7 日热度变化）", 12.5, False, INK, 3),
      ("③ 写入路径全部走 Service 层校验，不允许 Mapper 被跳过直接操作", 12.5, False, INK, 0)])

# ================= P16 建表策略 =================
s = add_slide(prs)
header(s, "16", "建表策略与启动自举", "为什么是 CREATE TABLE IF NOT EXISTS")
code_block(s, Inches(0.75), Inches(1.45), Inches(11.85), Inches(2.9), [
    ("@Bean  ApplicationContextAware → databaseBootstrap(appCtx) {", False),
    ("    // 容器启动完成时执行（应用层自举，非 SQL 脚本）", True),
    ("    for (String ddl : ALL_CREATE_TABLES)   // 14 张表 DDL", False),
    ("        jdbcTemplate.execute(ddl);         // IF NOT EXISTS 幂等", False),
    ("    if (spotDao.count() == 0) {            // 空库判定", False),
    ("        seedLoader.load();                 // 灌入 231 景点 + 335 评论", False),
    ("    }                                      // 换机器/清库零手工操作", False),
    ("}", False),
], size=12.5)
text(s, Inches(0.75), Inches(4.65), Inches(11.9), Inches(2.2),
     [("对比三种方案", 15, True, GREEN_D, 5),
      ("① SQL 脚本手工导入：换机器必出错，演示事故高发源", 12.5, False, INK, 3),
      ("② Flyway/Liquibase 迁移：课程项目过重，引入额外学习成本", 12.5, False, INK, 3),
      ("③ 启动自举（本项目）：幂等 + 自愈 —— 评委拿到任何一台空库机器，启动即完整可用", 12.5, True, GREEN_D, 0)])

# ================= P17 动态 SQL =================
s = add_slide(prs)
header(s, "17", "MyBatis 动态 SQL 解析", "防注入三道闸 + 手写物理分页")
code_block(s, Inches(0.75), Inches(1.45), Inches(11.85), Inches(3.5), [
    ("@Select(\"<script>SELECT * FROM scenic_spot WHERE status = 1\"", False),
    ("  + \"<if test='kw != null'> AND name LIKE CONCAT('%',#{kw},'%')</if>\"", False),
    ("  + \"<if test='province != null'> AND province = #{province}</if>\"", False),
    ("  + \" ORDER BY ${safeSort} LIMIT #{offset}, #{size}</script>\")", False),
    ("List<Spot> find(@Param(\"kw\") String kw, ..., @Param(\"safeSort\") String safeSort);", False),
    ("", False),
    ("// Service 层白名单 —— 用户输入永不直接进 SQL", True),
    ("String sort = switch (param) { case \"hot\" -> \"vr_count DESC\";", False),
    ("    case \"price\" -> \"price ASC\"; default -> \"rating DESC\"; };", False),
], size=12)
text(s, Inches(0.75), Inches(5.25), Inches(11.9), Inches(1.8),
     [("防注入三道闸", 15, True, GREEN_D, 5),
      ("① 条件值全走 #{} 预编译参数（PreparedStatement）  ② 结构关键字（ORDER BY 列名）走 ${} 但仅接收白名单映射值", 12.5, False, INK, 3),
      ("③ 分页参数 offset/size 由 Service 计算 clamp，防越界大分页拖库 —— 物理分页而非内存分页，231 条也按规范写", 12.5, False, INK, 0)])

# ================= P18 目录树 =================
s = add_slide(prs)
header(s, "18", "第四篇 · 项目文件架构", "完整目录树 —— 每个文件都有归属")
code_block(s, Inches(0.7), Inches(1.4), Inches(6.1), Inches(5.5), [
    ("untitled8/", True),
    ("├── pom.xml                    Maven 依赖", False),
    ("├── src/main/java/cn/edu/qut/", False),
    ("│   ├── config/     AppConfig / WebAppInitializer", False),
    ("│   ├── controller/ 14 个（Spot/Order/Ai/Vr/Pano/…）", False),
    ("│   ├── service/    28 个（接口 + Impl 成对）", False),
    ("│   ├── dao/        @Repository DAO 实现", False),
    ("│   ├── mapper/     12 个 MyBatis Mapper", False),
    ("│   ├── pay/        Alipay / WeChatPayService", False),
    ("│   ├── aspect/     AuditLogAspect 审计切面", False),
    ("│   └── entity/     实体类", False),
    ("├── src/main/resources/", False),
    ("│   ├── app/ai/alipay.properties   三大配置", False),
    ("│   └── schema 相关 DDL", False),
    ("└── src/main/webapp/", False),
    ("    ├── index.html   SPA 壳 + admin.html", False),
    ("    ├── WEB-INF/views/  14 个 JSP", False),
    ("    └── static/  js/css/lib/audio/pano 素材", False),
], size=11.5)
text(s, Inches(7.0), Inches(1.5), Inches(5.6), Inches(5.4),
     [("结构解读", 15, True, GREEN_D, 6),
      ("• 五层包结构 = 架构的物理映射：看包名即知职责，新人 10 分钟可定位任何功能", 12, False, INK, 5),
      ("• service 成对（接口 + Impl）：面向接口的实现解耦，是 Spring 生态的通用约定", 12, False, INK, 5),
      ("• pay / aspect 独立成包：支付与审计是横切关注点，与业务包物理隔离", 12, False, INK, 5),
      ("• static/pano 30 张 VR 六面贴图（每面 3100×3100），static/audio 背景音乐本地兜底", 12, False, INK, 0)])

# ================= P19 后端包解析 =================
s = add_slide(prs)
header(s, "19", "后端包结构职责详解", "115 个类怎么分工")
yy = table_rows(s, [
    ("包", "职责与代表类"),
    ("config（3）", "AppConfig 全量装配：数据源/事务/MyBatis/自举；WebAppInitializer 注册 DispatcherServlet；WebConfig MVC 配置"),
    ("controller（17）", "Spot/Order/Ai/Vr/Pano/Plan/User/Admin/Alipay/Jsp… REST + JSP 双出口，只做校验与转发"),
    ("service（30）", "接口 + Impl 成对：业务规则/状态机/故障转移/RAG 组装全在此层，@Transactional 声明处"),
    ("dao（9+9）", "接口 + @Repository 实现：注入 Mapper，加一层薄封装便于替换持久化技术"),
    ("mapper（13）", "MyBatis 注解 SQL + <script> 动态 SQL，防注入白名单，主键回填"),
    ("pay（4）", "AlipayPayService / WeChatPayService 接口+实现，策略模式双通道"),
    ("aspect（1）", "AuditLogAspect：@Around 拦截管理端写操作自动审计"),
    ("entity / dto", "实体与传输对象，Jackson 序列化直接复用"),
], rh=Inches(0.52), fs=11.5)
text(s, Inches(0.8), yy + Inches(0.15), Inches(11.7), Inches(0.5),
     [("命名规范：XxxController → XxxService → XxxServiceImpl → XxxDaoImpl → XxxMapper，五层同名前缀一查到底", 12, True, GREEN_D, 0)])

# ================= P20 前端资源 =================
s = add_slide(prs)
header(s, "20", "前端资源结构解析", "static/ 下的工程讲究")
card(s, Inches(0.75), Inches(1.45), Inches(5.85), Inches(2.3), "static/js · 约 2100 行",
     "app.js 单页应用核心：History 路由 / 12 板块渲染 /\nfetch 封装 / VR 三模式引擎 / 支付弹窗 / 地图页；\nqrcode.min.js 本地二维码库（不依赖 CDN）", 14.5, 11.5)
card(s, Inches(6.75), Inches(1.45), Inches(5.85), Inches(2.3), "static/css · app.css",
     "全站样式：票根式订单卡（打孔虚线）/ VR 全屏沉浸 /\n迷你播放器 / 玻璃拟态顶栏；[hidden] 兜底规则防弹层穿透", 14.5, 11.5)
card(s, Inches(0.75), Inches(3.95), Inches(5.85), Inches(2.3), "static/pano · VR 素材",
     "5 个景点 × 六面贴图（_f/_r/_b/_l/_u/_d.jpg），\n每面 3100×3100 约 2MB；缺贴图景点自动降级\n柱面环景/3D 图墙，永不破功能", 14.5, 11.5)
card(s, Inches(6.75), Inches(3.95), Inches(5.85), Inches(2.3), "static/lib + audio",
     "leaflet.js 本地化（校园网无 CDN 也能跑）；\naudio/ 本地国风 mp3 —— B 站嵌入曲断网时自动降级，\n配乐永不缺席", 14.5, 11.5)
text(s, Inches(0.75), Inches(6.5), Inches(11.9), Inches(0.6),
     [("资源工程化：静态资源全部带版本参数（index.html?v=20261001a）强刷缓存；全部本地化/可降级，弱网环境可用", 12.5, True, GREEN_D, 0)])

# ================= P21-27 IDEA 项目架构 · 一部分一部分深度解析 =================
def shot_fit(slide, fname, title, desc, ax=Inches(0.7), ay=Inches(1.35), aw=Inches(6.4), ah=Inches(5.65)):
    """任意宽高比截图等比缩放放入区域并居中 + 右侧深度解析"""
    path = os.path.join(DESK, fname)
    if os.path.exists(path):
        pic = slide.shapes.add_picture(path, ax, ay)
        scale = min(aw / pic.width, ah / pic.height)
        pic.width = Emu(int(pic.width * scale)); pic.height = Emu(int(pic.height * scale))
        pic.left = Emu(int(ax + (aw - pic.width) / 2)); pic.top = Emu(int(ay + (ah - pic.height) / 2))
    else:
        rect(slide, ax, ay, aw, ah, RGBColor(0xF0, 0xF4, 0xF2), line=GREEN, round_=True)
        text(slide, ax, ay + ah / 2 - Inches(0.4), aw, Inches(0.8),
             [("🖥️ IDEA 截图待补", 18, True, GREEN_D, 0)], align=PP_ALIGN.CENTER)
    lx = ax + aw + Inches(0.4)
    lw = SW - lx - Inches(0.55)
    lines = [(title, 17, True, GREEN_D, 6)]
    for seg in desc.split("\n"):
        lines.append((seg, 12, False, INK, 4))
    text(slide, lx, ay + Inches(0.05), lw, ah, lines)

idea_shots = [
    ("idea1_config_公共层.png", "21", "IDEA 架构 ① · 根包 / config / common / aop",
     "对应 IDE 左侧 cn.edu.qut 根目录 —— 地基与横切：\n• config ×3：AppConfig 全量装配（数据源/事务/MyBatis/启动自举）\n• WebAppInitializer 注解注册 DispatcherServlet，替代 web.xml\n• common ×3：Result 统一响应 / PageResult 分页 / BusinessException\n• aop ×1：AuditLogAspect 审计切面独立成包 —— 横切关注点物理隔离"),
    ("idea2_controller_控制层.png", "22", "IDEA 架构 ② · controller 控制层 ×17",
     "REST + JSP 双出口，每个类只做转发：\n• @RestController 纯 JSON 输出：Spot / Order / Ai / Vr / Pano / Recommend…\n• JspController 一家扛 15 条 /page/** 路由（JSTL + EL 服务端渲染）\n• AlipayController：同步 return + 异步 notify + trade.query 主动对账\n• 纪律：无业务规则、无 try/catch 泥潭，全局异常处理器兜底"),
    ("idea3_service_业务层.png", "23", "IDEA 架构 ③ · service 业务层 ×30",
     "接口 + 实现成对，全部业务规则的家：\n• 面向接口注入：Controller 依赖 XxxService，不碰 Impl\n• OrderServiceImpl：@Transactional 票务状态机（CAS 幂等更新）\n• AiAssistantServiceImpl：四引擎调度 + RAG 三重防线组装\n• TravelPlannerServiceImpl：偏好问卷 → AI 路线生成并落库"),
    ("idea4_dao_持久层.png", "24", "IDEA 架构 ④ · dao 持久层 ×18（接口 9 + 实现 9）",
     "薄封装隔离层：\n• @Repository 实现类注入 MyBatis Mapper，只做委托与微调\n• 价值：业务与 SQL 细节解耦，换持久化技术只动这一层\n• 命名链条：OrderService → OrderDao → TicketOrderMapper 一查到底"),
    ("idea5_mapper_pay_数据与支付.png", "25", "IDEA 架构 ⑤ · mapper ×13 与 pay ×4",
     "SQL 即接口 + 支付策略：\n• Mapper 全部注解 SQL + <script> 动态 SQL，#{} 预编译防注入\n• TicketOrderMapper：CAS 条件 UPDATE 支撑订单状态机\n• pay 双通道：AlipayPayService（真实沙箱 RSA2）+ WeChatPayService（完整契约），统一收敛到同一状态机"),
    ("idea6_entity_dto_实体层.png", "26", "IDEA 架构 ⑥ · entity ×13 与 dto",
     "数据模型层：\n• 13 个实体映射 14 张表（RouteSpot 为路线-景点关联实体）\n• TicketOrder 承载状态机字段：status / pay_method / pay_time\n• dto.Requests 隔离请求参数：入参不直接绑实体，防越权字段注入"),
    ("idea7_support_vo_支撑与资源.png", "27", "IDEA 架构 ⑦ · support / vo / resources",
     "支撑组件与配置资源：\n• LlmClient：AI 四引擎统一客户端（分级超时 + 故障转移）\n• web.GlobalExceptionHandler：全局异常 → 统一 {code,msg} 响应\n• LoginInterceptor 登录拦截 · SpotImageService 真实图片流水线\n• vo ×6 视图对象；resources：三大 properties + schema/data.sql 幂等自举"),
]
for fname, num, title, desc in idea_shots:
    s = add_slide(prs)
    header(s, num, title, "IDEA 左侧项目结构 · 一部分一部分深度解析")
    shot_fit(s, fname, "包结构解析", desc)

# ================= P28 功能篇分隔页 =================
divider(prs, "第五篇 · 功能演示", "FEATURES · 20 张运行截图全流程实景", "以下截图均为系统真实运行画面 · localhost:8080")

# ================= P25-44 运行截图页（20 张） =================
shots = [
    ("01_首页.png", "P29", "① SPA 首页", "Hero 大图 + 全局搜索 + 分类入口\nVR 环景舱 / 云游盲盒快捷按钮\n实时统计条：231 景点 / 16 路线 / 热度榜\n左下角 🔈 背景音乐（《国风·云游》免费商用循环）"),
    ("02_景点导览.png", "P30", "② 景点导览", "关键字 / 省份 / 排序 / 分页四维筛选\n卡片流：真实图片 + 评分 + 价格 + 标签\n排序走白名单映射 · LIMIT 物理分页"),
    ("10_景点详情.png", "P31", "③ 景点详情弹窗", "图集 + AI 讲解 + 一句话游记（引擎徽章）\nVR 全景入口 + 收藏 + 购票一条龙\n80 字真实简介来自数据库，非 AI 编造"),
    ("20_登录弹窗.png", "P32", "④ 登录 / 注册", "登录 / 注册一体弹窗\nHttpSession 会话保持\n收藏 / 订单 / 打卡均需登录态校验"),
    ("11_支付弹窗.png", "P33", "⑤ 三通道支付弹窗", "微信（契约完整）/ 支付宝沙箱 / 模拟兜底\n支付宝走真实 RSA2 签名 + trade.query 主动对账\n下单即建 PENDING 订单，支付成功触发状态机"),
    ("06_我的订单.png", "P34", "⑥ 我的订单 · 票根凭证", "票根式订单卡：票存根 + 打孔虚线 + 二维码凭证\n状态筛选 / 退款 / 删除全操作\n状态流转 CAS 条件更新，幂等防重"),
    ("05_虚拟游览.png", "P35", "⑦ 虚拟游览", "主题路线时间轴式云游\n看地图 → 听讲解 → 环景环视 → 打卡 → 下一站\n打卡数据实时落库反哺热度榜"),
    ("12_VR环景舱.png", "P36", "⑧ VR 真 360° 环景舱", "六面 3100×3100 实拍贴图 CSS 3D 立方体投影\n拖拽环视（±85° 俯仰）/ 滚轮变焦 / 全屏\n进舱自动配乐 + AI 讲解，打卡落库"),
    ("08_景点地图.png", "P37", "⑨ 景点地图", "Leaflet 1.9.4 本地化 + 高德瓦片\n231 个真实坐标全量标注\n悬停看名称 · 点击弹详情 · 直达购票"),
    ("04_智能推荐.png", "P38", "⑩ 智能推荐", "热门（打卡聚合）/ 高分（评分池）/ 偏好匹配（标签加权）\n三维推荐由 SQL 聚合 + Service 加权实现\n打卡越多推荐越准 —— 数据闭环"),
    ("03_智能规划.png", "P39", "⑪ AI 智能规划", "8 字段偏好问卷 → AI 四引擎生成逐日路线\nRAG 约束：只能从 231 真实景点中选\n结果落库 travel_plan，同偏好毫秒级复用"),
    ("07_AI助教.png", "P40", "⑫ AI 助教", "景点问答 / 讲解 / 游记生成\n徽章如实显示引擎：⚡DeepSeek/通义/豆包/📚本地\n四引擎故障转移 + 分级超时（10s/8s/15s）"),
    ("13_JSP首页.png", "P41", "⑬ JSP 传统视图", "JSTL + EL 服务端渲染 —— 传统 SSM 视图\n与 SPA 共用同一 Service 层（双视图架构）\n15 条 /page/** 路由：排行 / 统计 / 指南 / 收藏 / 关于"),
    ("14_排行榜.png", "P42", "⑭ 热度排行榜 JSP", "热度榜 = vr_view_log GROUP BY 聚合\n不存冗余计数字段，查询时聚合保证一致\n打卡行为直接驱动榜单"),
    ("15_省份指南.png", "P43", "⑮ 省份指南 JSP", "31 省级行政区分区导览（/page/provinces）\n嵌套 c:forEach：省份 → 景点列表两级渲染\n空间导览四级钻取动线的起点"),
    ("16_数据统计.png", "P44", "⑯ 数据统计 JSP", "服务端聚合统计页\n评分分布 / 省份覆盖 / 免费占比等指标\n聚合逻辑全部在 SQL/Service 层完成"),
    ("17_收藏夹.png", "P45", "⑰ 收藏夹 JSP", "登录态收藏列表（user_id + spot_id 双外键）\n一键取消收藏 / 直达详情"),
    ("18_关于.png", "P46", "⑱ 关于 JSP", "项目愿景与技术栈说明页\n设计原则：数据真实 / 永不中断 / 双视图 / 零侵入审计"),
    ("09_我的.png", "P47", "⑲ 个人中心", "足迹 / 收藏 / 偏好 / 订单汇总\nVR 打卡足迹与热度榜数据同源"),
    ("19_管理端.png", "P48", "⑳ 管理后台", "admin.html 管理端单页\n景点 / 订单 / 用户 CRUD 全覆盖\n所有写操作被 @Aspect 自动审计落库"),
]
for fname, num, title, desc in shots:
    s = add_slide(prs)
    header(s, num, title, "运行截图 · 真实运行画面")
    shot(s, fname, "功能说明", desc)

# ================= P32 技术篇分隔页 =================
divider(prs, "第六篇 · 技术深度解析", "DEEP DIVE · 八大核心机制逐行拆解", "不止“用了什么”，更讲清“为什么这样用”")

# ================= P33 AI 四引擎详解 =================
s = add_slide(prs)
header(s, "50", "AI 四引擎故障转移 · 总体设计", "可用性工程的完整实践")
text(s, Inches(0.8), Inches(1.4), Inches(11.7), Inches(0.9),
     [("DeepSeek ──失败──▶ 通义千问 ──失败──▶ 豆包 ──失败──▶ 本地规则库", 19, True, GREEN_D, 0)], align=PP_ALIGN.CENTER)
card(s, Inches(0.75), Inches(2.35), Inches(2.9), Inches(2.5), "为什么要多引擎",
     "单一 AI API 有三类风险：\n服务端 5xx / 限流 429\n网络抖动超时\n账户额度耗尽", 14, 12)
card(s, Inches(3.8), Inches(2.35), Inches(2.9), Inches(2.5), "分级超时设计",
     "DeepSeek 10s（质量优先）\nqwen-turbo 8s（实测最快）\n豆包 15s（长尾兜底）\n按引擎实测耗时定阈值", 14, 12)
card(s, Inches(6.85), Inches(2.35), Inches(2.9), Inches(2.5), "本地规则兜底",
     "基于 MySQL 真实景点\n模板化生成回答\n零外部依赖，永不失败", 14, 12)
card(s, Inches(9.9), Inches(2.35), Inches(2.75), Inches(2.5), "透明展示",
     "前端徽章如实标注来源\n⚡DeepSeek / ⚡通义\n⚡豆包 / 📚本地知识库", 14, 12)
rect(s, Inches(0.75), Inches(5.15), Inches(11.9), Inches(1.7), GREEN_L, round_=True)
text(s, Inches(1.05), Inches(5.32), Inches(11.3), Inches(1.4),
     [("判定降级的三个信号", 14.5, True, GREEN_D, 4),
      ("HTTP 层：非 200 / 429 / 5xx    网络层：connect/read 超时    内容层：响应空或 JSON 解析失败 —— 任一触发即切换下一引擎", 12.5, False, INK, 0)])

# ================= P34 LlmClient 代码 =================
s = add_slide(prs)
header(s, "51", "AI 引擎抽象 · LlmClient 与 LlmDispatcher", "面向接口 + 策略模式的教科书实现")
code_block(s, Inches(0.75), Inches(1.45), Inches(11.85), Inches(4.3), [
    ("public interface LlmClient {                    // 引擎统一契约", False),
    ("    LlmResult chat(String system, String user, int timeoutMs);", False),
    ("}", False),
    ("", False),
    ("public LlmResult dispatch(String sys, String user) {   // 调度器", False),
    ("    for (LlmClient c : clients) {              // 按配置顺序尝试", False),
    ("        try {", False),
    ("            LlmResult r = c.chat(sys, user, c.timeout());", False),
    ("            if (r.ok()) return r;              // 成功即返回", False),
    ("        } catch (Exception e) { /* 记日志，继续下一引擎 */ }", False),
    ("    }", False),
    ("    return localFallback.chat(sys, user, 0);   // 本地规则兜底", False),
    ("}", False),
], size=12.5)
text(s, Inches(0.75), Inches(6.0), Inches(11.9), Inches(1.1),
     [("扩展性：新增引擎（如 Kimi/智谱）= 新写一个 LlmClient 实现 + 配置加一个名字，调度器与业务层零改动", 12.5, True, GREEN_D, 0)])

# ================= P35 RAG 详解 =================
s = add_slide(prs)
header(s, "52", "RAG 数据约束 · 三重防线", "让 AI 只说真话")
stages = [
    ("① 召回 Retrieve", "按用户问题/规划条件\n从 231 真实景点检索\n候选集（省份/标签/评分过滤）"),
    ("② 注入 Augment", "候选集结构化写入 Prompt\n+ 硬约束指令：\n「只能从候选列表选择，禁止编造」"),
    ("③ 校验 Verify", "AI 输出景点名逐个回库比对\n库外条目剔除\n必要时降级重试"),
]
x = Inches(0.75)
for t, d in stages:
    card(s, x, Inches(1.5), Inches(3.9), Inches(2.2), t, d, 15, 12)
    x += Inches(4.05)
code_block(s, Inches(0.75), Inches(4.0), Inches(11.85), Inches(2.2), [
    ("// Prompt 组装（PlanService 内节选）", True),
    ("prompt.append(\"你只能从下方【候选景点列表】中选择，禁止编造任何不存在的景点。\\n\");", False),
    ("prompt.append(\"输出严格 JSON：{days:[{day, spots:[{name, reason, cost}]}]}\\n\");", False),
    ("prompt.append(\"候选数据（MySQL 实时查询）: \").append(jsonOf(spots));", False),
], size=11.5)
text(s, Inches(0.75), Inches(6.45), Inches(11.9), Inches(0.6),
     [("效果：讲解/规划/游记全部内容可回溯到数据库真实景点 —— 幻觉从“概率问题”变成“工程问题”被解决", 12.5, True, GREEN_D, 0)])

# ================= P36 AI 超时实测 =================
s = add_slide(prs)
header(s, "53", "AI 引擎选型 · 实测数据驱动", "没有实测就没有发言权")
yy = table_rows(s, [
    ("引擎 / 模型", "实测表现"),
    ("qwen-plus（弃用）", "600-800 字讲解生成 15.7s，超过 12s 上限必超时 —— 用户视角等于失败"),
    ("qwen-turbo（采用）", "对话 0.9s · 400 字讲解 3.9s —— 速度约为 plus 的 4 倍，讲解任务主力"),
    ("deepseek-chat", "质量最优（规划/长文），但 8~10s 耗时 → 定位主力规划引擎"),
    ("豆包 ep 接入", "长尾兜底；曾实测 29.5s 思维链 → 配置 no-thinking 关闭后改善"),
    ("本地规则库", "0s —— 断网/全故障场景下 100% 可用的最后防线"),
], rh=Inches(0.56), fs=12)
text(s, Inches(0.8), yy + Inches(0.18), Inches(11.7), Inches(1.5),
     [("超时阈值怎么定", 15, True, GREEN_D, 5),
      ("阈值 = 实测 P95 耗时 × 2（留余量）：qwen-turbo 8s / DeepSeek 10s / 豆包 15s。过短（如 2s）会造成“假失败”反而更慢，过长则用户体验劣化 —— 这组数字是踩坑踩出来的", 12.5, False, INK, 0)])

# ================= P37 VR 投影原理 =================
s = add_slide(prs)
header(s, "54", "VR 立方体投影 · 数学原理", "纯 CSS 3D，零 WebGL 依赖")
code_block(s, Inches(0.75), Inches(1.45), Inches(6.3), Inches(4.3), [
    ("// buildSphere() —— 相机置于立方体中心", True),
    ("const F = max(innerWidth, innerHeight) * 1.06;", False),
    ("const R = F / 2;                    // 半边长", False),
    ("const face = (url, tf) => `<div", False),
    ("   class=\"vr-sface\"", False),
    ("   style=\"background-image:url(${url});", False),
    ("     transform:${tf} translateZ(${-R}px)\"/>`;", False),
    ("// 六面：rotateY 0/-90/180/90 + rotateX ∓90", False),
    ("// 俯仰/环视 = 旋转整个 .vr-sphere 容器", False),
    ("// FOV 变焦：perspective = (W/2) / tan(hfov/2)", False),
], size=11.5)
text(s, Inches(7.3), Inches(1.5), Inches(5.35), Inches(5.3),
     [("为什么用 CSS 而不是 Three.js", 15, True, GREEN_D, 6),
      ("• 零依赖：不需要 WebGL 库与着色器，加载快、兼容性好", 12, False, INK, 4),
      ("• 六面 div + transform 即立方体，浏览器 GPU 合成加速", 12, False, INK, 4),
      ("• FOV 视野角与 perspective 互算：拖拽/滚轮/陀螺仪全部收敛为“旋转容器 + 调 perspective”两个原子操作", 12, False, INK, 4),
      ("• 实测：3100×3100 六面贴图流畅 60fps", 12, False, INK, 0)])

# ================= P38 VR 三模式 =================
s = add_slide(prs)
header(s, "55", "VR 三模式渲染与交互实现", "自动探测 · 原地热切换")
yy = table_rows(s, [
    ("模式", "实现与触发"),
    ("sphere 真 360°", "GET /api/pano/{spotId} 返回六面贴图 URL → 自动切入；俯仰 ±85°、FOV 50°~115°"),
    ("pano 柱面环景", "无立方体贴图但有宽幅实拍图 → 柱面排布环绕"),
    ("cube 3D 图墙", "兜底模式：景点图片 3D 环绕 —— 保证任何景点都能进 VR"),
    ("热切换", "三模式原地切换，配乐/AI 讲解/打卡状态全程保持不重置"),
], rh=Inches(0.56), fs=12)
card(s, Inches(0.8), yy + Inches(0.25), Inches(5.7), Inches(1.55), "交互实现",
     "拖拽环视（惯性甩动，速度衰减 0.94）\n滚轮变焦 · 双指捏合 · 手机陀螺仪\n点按钮自动请求浏览器全屏", 14, 11.5)
card(s, Inches(6.85), yy + Inches(0.25), Inches(5.7), Inches(1.55), "沉浸配套",
     "AI 讲解进舱即播 + 语音朗读\n配乐三选一随机注入，退出自动释放\n打卡数据实时落库", 14, 11.5)

# ================= P39 VR 闭环 =================
s = add_slide(prs)
header(s, "56", "VR 全栈数据闭环", "一次打卡，贯穿 SSM 五层")
code_block(s, Inches(0.75), Inches(1.45), Inches(11.85), Inches(3.2), [
    ("VR 环景舱内点击「📍 打卡」", False),
    ("  ↓ POST /api/vr/view  {spotId, userId, duration}", False),
    ("  ↓ VrController.view()          @RestController 参数绑定", False),
    ("  ↓ VrService.record()           @Service 业务校验（景点存在/用户有效）", False),
    ("  ↓ VrViewLogMapper.insert()     @Mapper 主键回填", False),
    ("  ↓ MySQL vr_view_log 落库", False),
    ("  → 首页热度榜 = SELECT spot_id, COUNT(*) GROUP BY spot_id → 实时更新", False),
], size=13)
text(s, Inches(0.75), Inches(5.0), Inches(11.9), Inches(1.9),
     [("闭环意义", 15, True, GREEN_D, 5),
      ("用户行为（打卡）→ 数据落库 → 聚合计算 → 反哺推荐排序 → 影响下一个用户的“热门推荐”—— 数据在系统内真实流动，而非静态展示", 12.5, False, INK, 3),
      ("这正是“全栈”的含义：前端交互与后端存储不是两座孤岛，而是一条完整的数据链路", 12.5, True, GREEN_D, 0)])

# ================= P40 状态机代码 =================
s = add_slide(prs)
header(s, "57", "事务状态机 · 代码级解析", "@Transactional + CAS 条件更新")
code_block(s, Inches(0.75), Inches(1.45), Inches(11.85), Inches(4.0), [
    ("@Transactional   // OrderServiceImpl", False),
    ("public void markPaid(String orderNo, String payMethod) {", False),
    ("    // CAS 更新：只有 PENDING → PAID 会成功，重复回调 rows=0", True),
    ("    int rows = orderDao.casUpdate(", False),
    ("        \"UPDATE ticket_order SET status='PAID', pay_time=NOW(),", False),
    ("                    pay_method=#{m} WHERE order_no=#{no} AND status='PENDING'\");", False),
    ("    if (rows == 0) throw new OrderStateException(\"重复支付或状态冲突\");", False),
    ("    // 同一事务内：写支付流水 / 触发出票凭证 …… 原子生效", False),
    ("}", False),
], size=12.5)
text(s, Inches(0.75), Inches(5.7), Inches(11.9), Inches(1.4),
     [("三个关键点", 15, True, GREEN_D, 5),
      ("① @Transactional：多表写操作同生共死  ② CAS 条件：幂等防重，无需分布式锁  ③ 状态约束：非法流转被 WHERE 条件天然拒绝", 12.5, False, INK, 0)])

# ================= P41 支付详解 =================
s = add_slide(prs)
header(s, "58", "双通道支付 · 策略模式落地", "真实沙箱联调 + 优雅降级")
yy = table_rows(s, [
    ("通道", "实现要点"),
    ("AlipayPayService（真实沙箱）", "RSA2 签名 · wap.pay/precreate 下单 · trade.query 主动对账 · notify 回调 rsaCheckV2 验签"),
    ("WeChatPayService（契约完整）", "createOrder / queryTradeStatus / verifyNotifySign 接口齐备，可无缝替换真实商户实现"),
    ("SANDBOX 模拟通道", "零网络依赖兜底，演示永不翻车"),
], rh=Inches(0.6), fs=12)
rect(s, Inches(0.8), yy + Inches(0.25), Inches(11.7), Inches(1.9), GREEN_L, round_=True)
text(s, Inches(1.1), yy + Inches(0.42), Inches(11.1), Inches(1.6),
     [("校园网回调难题的解法", 14.5, True, GREEN_D, 4),
      ("支付宝异步通知需要公网 HTTPS 地址 —— 校园网 NAT 环境不可达。解法：不依赖回调，前端支付后每 3 秒轮询 /alipay-query，", 12.5, False, INK, 2),
      ("后端调 alipay.trade.query 主动查单，TRADE_SUCCESS 即触发状态机 —— “主动对账”替代“被动回调”，闭环成立", 12.5, True, GREEN_D, 0)])

# ================= P42 AOP 代码 =================
s = add_slide(prs)
header(s, "59", "AOP 零侵入审计 · 切面代码解析", "横切关注点与业务彻底解耦")
code_block(s, Inches(0.75), Inches(1.45), Inches(11.85), Inches(3.8), [
    ("@Aspect @Component", False),
    ("public class AuditLogAspect {", False),
    ("  @Around(\"execution(* cn.edu.qut.controller.admin..*(..))\")", False),
    ("  public Object audit(ProceedingJoinPoint pjp) throws Throwable {", False),
    ("      long t0 = System.currentTimeMillis();", False),
    ("      Object result = pjp.proceed();          // 放行原业务", False),
    ("      auditLogDao.insert(AdminId(), opType(pjp), target(pjp),", False),
    ("                         args(pjp), took(t0)); // 自动落审计表", False),
    ("      return result;", False),
    ("  }", False),
    ("}", False),
], size=12.5)
text(s, Inches(0.75), Inches(5.55), Inches(11.9), Inches(1.5),
     [("价值解读", 15, True, GREEN_D, 5),
      ("管理端 20+ 个写操作接口，无一处手写日志代码 —— 审计职责完全收敛在切面。新增管理接口自动纳入审计，零维护成本；这正体现 Spring AOP“声明式横切”的工程价值", 12.5, False, INK, 0)])

# ================= P43 双视图 + 地图 =================
s = add_slide(prs)
header(s, "60", "双视图渲染 与 地图实现", "一套 Service 两种输出 · 231 坐标一图尽览")
card(s, Inches(0.75), Inches(1.5), Inches(5.85), Inches(2.4), "双视图机制",
     "SPA：@RestController → JSON → fetch 渲染\nJSP：@Controller → Model + JSTL/EL 渲染\n两者注入同一个 XxxService —— 业务零复制\n含义：前端形态可替换，后端稳如磐石", 14.5, 11.5)
card(s, Inches(6.75), Inches(1.5), Inches(5.85), Inches(2.4), "Leaflet 地图实现",
     "/api/spots/map 全量坐标接口\nLeaflet 本地化 + 高德瓦片（多源降级）\n231 个 circleMarker 视口裁剪渲染\n悬停 tooltip → 点击 popup 详情 → 直达详情页", 14.5, 11.5)
rect(s, Inches(0.75), Inches(4.25), Inches(11.9), Inches(2.5), GREEN_L, round_=True)
text(s, Inches(1.05), Inches(4.45), Inches(11.3), Inches(2.2),
     [("空间导览动线", 15, True, GREEN_D, 5),
      ("全国分布（地图页）→ 省份聚焦（省份指南）→ 单点详情（景点页）→ 实景体验（VR）—— 四级钻取成完整动线", 12.5, False, INK, 3),
      ("工程细节：Leaflet JS/CSS 全部下载到 static/lib 本地部署 —— 校园网就算连不上 CDN，地图照常可用（与 AI 本地兜底同一设计哲学：永不因外部依赖而不可用）", 12.5, True, GREEN_D, 0)])

# ================= P44 统计与难点 =================
s = add_slide(prs)
header(s, "61", "工作量统计与难点攻克", "体量证明投入 · 难点证明深度")
row1 = [("115", "Java 类"), ("7,128", "Java 行"), ("50+", "接口"), ("15", "JSP 路由")]
row2 = [("14", "数据表"), ("231", "景点"), ("335", "评论"), ("8", "创新点")]
y = Inches(1.4)
for row in (row1, row2):
    x = Inches(0.9)
    for n, lb in row:
        stat_block(s, x, y, Inches(2.72), Inches(1.25), n, lb)
        x += Inches(2.92)
    y += Inches(1.45)
probs = [
    ("JDK 25 × javax.* 冲突", "升级 Spring 7 + Jakarta + Tomcat 11"),
    ("AI 幻觉", "RAG 召回 + 硬约束 + 回库校验"),
    ("AI 引擎故障", "四引擎转移 + 分级超时 + 本地兜底"),
    ("回调不可达", "trade.query 主动对账替代回调"),
]
x = Inches(0.9)
for t, sol in probs:
    card(s, x, Inches(4.45), Inches(2.85), Inches(2.2), t, sol, 12.5, 11)
    x += Inches(2.98)

# ================= P45 总结致谢 =================
s = add_slide(prs)
rect(s, 0, 0, SW, SH, GREEN_D)
rect(s, 0, 0, SW, Pt(4), GOLD)
text(s, Inches(1.0), Inches(0.8), Inches(11.3), Inches(0.8), [("总结与展望", 34, True, WHITE, 0)])
text(s, Inches(1.0), Inches(1.85), Inches(11.3), Inches(2.0),
     [("项目以 SSM 五层架构完成数据、服务、接口到交互的完整闭环，", 15.5, False, WHITE, 4),
      ("八个创新点均有真实代码与实测数据支撑，服务可用性以“永不中断”为设计底线。", 15.5, False, WHITE, 10),
      ("展望：引入 Redis 缓存热点 · VR 覆盖全部 231 景点 · 支付升级正式商户", 13, False, RGBColor(0xCF, 0xF0, 0xE2), 0)])
rect(s, Inches(1.0), Inches(3.95), Inches(11.3), Inches(1.5), RGBColor(0x06, 0x3D, 0x2B), round_=True)
text(s, Inches(1.35), Inches(4.12), Inches(10.6), Inches(1.2),
     [("从家乡滨州的黄河故道，到全国 31 个省份的名山大川；", 14.5, False, WHITE, 3),
      ("「云游中国」希望用 231 个真实景点、VR 实景与受约束的 AI 讲解，把中国的山河讲给更多人听。", 14.5, True, GOLD, 0)])
text(s, Inches(1.0), Inches(5.85), Inches(11.3), Inches(1.0),
     [("谢谢观看 · 恳请各位老师批评指正", 30, True, WHITE, 0)], align=PP_ALIGN.CENTER)

out = os.path.join(DESK, "云游中国-答辩PPT-62页.pptx")
prs.save(out)
print("已生成:", out, "共", len(prs.slides._sldIdLst), "页")
