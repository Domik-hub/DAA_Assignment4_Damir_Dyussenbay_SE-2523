"""Build the four-page report solely from the recorded Java experiment."""
from pathlib import Path
from collections import defaultdict
from statistics import median
import csv
import math
import re
import xml.etree.ElementTree as ET

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "results"
PLOTS = RESULTS / "plots"
PLOTS.mkdir(parents=True, exist_ok=True)
with (RESULTS / "results.csv").open(newline="", encoding="utf-8") as f:
    rows = list(csv.DictReader(f))
groups = defaultdict(list)
for row in rows:
    groups[row["algorithm"], row["input"], int(row["n"])].append(row)
assert len(rows) == 330 and len(groups) == 66, (len(rows), len(groups))
stats = {}
counter_fields = ["recursive_calls", "dp_cells", "comparisons", "lps_comparisons", "total_comparisons", "aux_cells", "result"]
for key, samples in groups.items():
    assert len(samples) == 5 and {int(s["run"]) for s in samples} == set(range(1, 6))
    for field in counter_fields:
        assert len({s[field] for s in samples}) == 1, (key, field)
    times = [float(s["time_ms"]) for s in samples]
    assert all(t > 0 and math.isfinite(t) for t in times)
    for s in samples:
        assert int(s["seed"]) == 20261107
        assert abs(float(s["time_ms"]) * int(s["iterations"]) - float(s["batch_time_ms"])) < 0.00001
        assert int(s["total_comparisons"]) == int(s["comparisons"]) + int(s["lps_comparisons"])
    stats[key] = {field: int(samples[0][field]) for field in counter_fields}
    stats[key].update(median_ms=median(times), min_ms=min(times), max_ms=max(times))
for algorithm in {k[0] for k in stats}:
    sizes = {k[2] for k in stats if k[0] == algorithm}
    assert len(sizes) >= 4 and max(sizes) >= 4 * min(sizes)
for input_type in ("identical", "random", "disjoint"):
    for n in (128, 256, 512, 1024, 2048):
        assert stats["Table", input_type, n]["result"] == stats["TwoRows", input_type, n]["result"]
        assert stats["Table", input_type, n]["dp_cells"] == n * n
        assert stats["TwoRows", input_type, n]["dp_cells"] == n * n
for input_type in ("random", "early_mismatch", "adversarial"):
    for n in (4096, 16384, 65536, 262144):
        assert stats["Naive", input_type, n]["result"] == stats["KMP", input_type, n]["result"]

with (RESULTS / "summary.csv").open("w", newline="", encoding="utf-8") as f:
    fields = ["algorithm", "input", "n", "median_ms", "min_ms", "max_ms"] + counter_fields
    writer = csv.DictWriter(f, fieldnames=fields)
    writer.writeheader()
    for (algorithm, kind, n), values in sorted(stats.items()):
        writer.writerow(dict(algorithm=algorithm, input=kind, n=n, **values))

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 8.5,
                     "axes.titlesize": 11, "axes.labelsize": 8.5, "legend.fontsize": 7,
                     "axes.spines.top": False, "axes.spines.right": False,
                     "savefig.facecolor": "white"})
BLUE, ORANGE, GREEN = "#17699e", "#c35a23", "#23816d"

def base_plot(title, ylabel, xlog=False, height=2.55):
    fig, ax = plt.subplots(figsize=(7.1, height))
    fig.subplots_adjust(left=0.115, right=0.98, bottom=0.23, top=0.85)
    ax.set_title(title, loc="left", fontweight="bold", pad=10)
    ax.set_xlabel("n (UTF-16 code units)")
    ax.set_ylabel(ylabel)
    ax.set_yscale("log")
    if xlog:
        ax.set_xscale("log", base=2)
    ax.grid(True, which="major", color="#dbe2e7", linewidth=0.6)
    ax.set_axisbelow(True)
    return fig, ax

fig, ax = base_plot("Recursive edit distance: repeated work", "Recursive calls (count)")
for kind, color, marker in [("identical", GREEN, "o"), ("random", BLUE, "s"), ("disjoint", ORANGE, "^")]:
    xs = [3, 6, 9, 12]
    ax.plot(xs, [stats["Recursive", kind, n]["recursive_calls"] for n in xs], label=kind, color=color, marker=marker)
ax.set_xticks(xs)
ax.legend(loc="upper left", ncol=3, framealpha=.95)
fig.savefig(PLOTS / "01_recursive_calls.png", dpi=220)
plt.close(fig)

fig, ax = base_plot("DP storage: full table versus two rows", "Allocated integer cells (count)", True)
xs = [128, 256, 512, 1024, 2048]
for algorithm, color, marker in [("Table", BLUE, "s"), ("TwoRows", GREEN, "o")]:
    ax.plot(xs, [stats[algorithm, "random", n]["aux_cells"] for n in xs], label=algorithm, color=color, marker=marker)
ax.set_xticks(xs, [str(n) for n in xs])
ax.legend(loc="upper left")
fig.savefig(PLOTS / "02_dp_storage.png", dpi=220)
plt.close(fig)

for filename, title, field, ylabel in [
    ("03_search_comparisons.png", "String search: comparisons including LPS", "total_comparisons", "Character comparisons (count)"),
    ("04_search_time.png", "String search: median of five timed batches", "median_ms", "Time per invocation (ms)")]:
    fig, ax = base_plot(title, ylabel, True, height=2.9)
    fig.subplots_adjust(top=0.66, bottom=0.20)
    ax.set_title(title, loc="left", fontweight="bold", pad=49)
    xs = [4096, 16384, 65536, 262144]
    for algorithm, color in [("Naive", ORANGE), ("KMP", BLUE)]:
        for kind, style, marker in [("random", "-", "o"), ("early_mismatch", ":", "s"), ("adversarial", "--", "^")]:
            label = f"{algorithm}: {kind.replace('early_mismatch', 'early mismatch')}"
            ax.plot(xs, [stats[algorithm, kind, n][field] for n in xs], label=label, color=color, linestyle=style, marker=marker, markersize=4)
    ax.set_xticks(xs, ["4,096", "16,384", "65,536", "262,144"])
    ax.legend(loc="lower left", bbox_to_anchor=(0, 1.02), ncol=3,
              borderaxespad=0, frameon=False, columnspacing=1.4)
    fig.savefig(PLOTS / filename, dpi=220)
    plt.close(fig)

def s(alg, kind, n): return stats[alg, kind, n]
def number(n): return f"{n:,}"
def ms(t): return f"{t:.6f}"
rd = s("Recursive", "disjoint", 12)
ri = s("Recursive", "identical", 12)
tr = s("Table", "random", 2048)
tw = s("TwoRows", "random", 2048)
na = s("Naive", "adversarial", 262144)
ka = s("KMP", "adversarial", 262144)
memory_ratio = tr["aux_cells"] / tw["aux_cells"]
comp_ratio = na["total_comparisons"] / ka["total_comparisons"]
time_ratio = na["median_ms"] / ka["median_ms"]
slow_search_key = max((k for k in stats if k[0] in ("Naive", "KMP")), key=lambda k: stats[k]["median_ms"])
slow_search = stats[slow_search_key]
plot_captions = [
    f"Disjoint strings require {number(rd['recursive_calls'])} calls at n=12, compared with {number(ri['recursive_calls'])} for identical strings; the logarithmic axis exposes the growth gap.",
    f"At n=2048, TwoRows uses {number(tw['aux_cells'])} integer cells versus {number(tr['aux_cells'])} for Table, a {memory_ratio:.1f}x reduction; both still evaluate {number(tr['dp_cells'])} interior states.",
    f"At n=262144 and m=64, adversarial Naive makes {comp_ratio:.2f}x as many comparisons as KMP including LPS, while both curves are linear in n for this fixed m.",
    f"On the largest adversarial input, the measured medians are {ms(na['median_ms'])} ms for Naive and {ms(ka['median_ms'])} ms for KMP ({time_ratio:.2f}x); random and early-mismatch inputs show why input type matters."
]

card = [
    ("Questions", "Edit distance: how do structure and size affect recursive work, DP time and storage? Search: how much does a long nearly matching prefix hurt Naive relative to KMP?"),
    ("Sizes and why", "Recursive n = 3, 6, 9, 12 (4x span, bounded exponential work). Table/TwoRows n = 128, 256, 512, 1024, 2048 (16x). Naive/KMP text n = 4096, 16384, 65536, 262144 (64x); pattern m = 64. The larger sizes expose scaling without an unbounded run."),
    ("Purposeful inputs", "Edit: identical random strings expose the equal-character shortcut; independent ABCD strings give a baseline; A^n versus B^n forces all recursive branches. Search: random ABCD is the baseline; A^n versus B^64 fails immediately; A^n versus A^63 B forces late mismatches at every Naive alignment."),
    ("Metrics", "Measured inside the code: recursive calls, interior DP cells, character comparisons, and allocated integer cells. Search comparisons include both the scan and KMP LPS construction. Also record elapsed time and report the median of five runs. Counters explain work independently of the clock."),
    ("Protocol", "Custom fixed-seed generator, seed 20261107; same input across algorithms of the same size. Inputs are generated before timing. Two warm-up batches, then five recorded batches; time_ms = batch_time_ms / iterations. Recursion batches: 10000, 100, 2, 1; DP: 64, 16, 4, 1, 1; search: 256, 64, 16, 4. Counters are per invocation and checked between batches."),
    ("Prediction", "Disjoint recursion grows exponentially; identical recursion is linear. Both DPs evaluate n^2 states, with quadratic versus linear integer storage. Naive is costly on late mismatches; KMP remains O(n+m). With fixed m, both searches grow linearly in n but have different constants. Naive can win on immediate mismatches."),
    ("Planned plots", "Recursive calls (log y): repeated work. DP cells allocated (log-log): storage growth. Search comparisons (log-log): sensitivity to input. Median search milliseconds (log-log): agreement and differences between clock and counters."),
    ("Reproducibility", "Inputs, results and counters repeat exactly; elapsed time cannot. The design was committed before the first benchmark. No experiment sizes, input types or predictions were changed after measurements. This is an instrumented experiment, not a JMH benchmark.")
]

edit_result = (
    f"Prediction versus result. Disjoint recursion increased from {number(s('Recursive','disjoint',3)['recursive_calls'])} calls at n=3 to {number(rd['recursive_calls'])} at n=12, whereas identical inputs used n+1 calls. "
    f"Both DP variants measured n^2 interior states for every input type; the predicted quadratic/linear storage difference also held. "
    f"For random n=2048, Table took {ms(tr['median_ms'])} ms and TwoRows {ms(tw['median_ms'])} ms. "
    "Timings need not grow smoothly because compilation, allocation and cache effects differ even when operation counts match. The counters support the complexity claim more directly than a timing ranking."
)
search_result = (
    f"Prediction versus result. At the largest adversarial size, Naive counted {number(na['comparisons'])} scan comparisons; KMP counted {number(ka['comparisons'])} scan plus {number(ka['lps_comparisons'])} LPS comparisons. "
    "These match (n-m+1)*m for Naive and near-2n scan work for KMP on this constructed input. "
    "The long prefix caused the predicted penalty. Because m=64 is fixed, these measurements establish different constants along linear curves; they do not empirically establish quadratic growth in n. "
    "A separate experiment varying m with n would be needed for that claim. "
    "One surprise: Naive made slightly fewer comparisons on early mismatches, yet KMP had a lower median at all four sizes; comparison counts omit loop bookkeeping and JIT effects, so fewer comparisons need not mean lower elapsed time."
)
answers = [
    ("1. Which input made search slowest, and why?",
     f"The slowest measured search configuration was {slow_search_key[0]} on {slow_search_key[1].replace('_',' ')} input at n={slow_search_key[2]}, with median {ms(slow_search['median_ms'])} ms. "
     "The constructed A^n text and A^63 B pattern force Naive to compare 64 characters at every feasible starting position before failing. "
     "KMP reuses the border information after each mismatch and does not restart that long prefix. "
     "This worst-case input explains the work counts, although timing order among cheaper cases can still depend on the machine."),
    ("2. How large an n could recursive edit distance handle?",
     f"The largest tested length was n=12, which completed on all three input types; the disjoint case needed {number(rd['recursive_calls'])} calls and median {ms(rd['median_ms'])} ms. "
     "The stopping point was the preselected experiment limit, not an observed crash or timeout. "
     "Repeated branching makes larger disjoint inputs increasingly expensive, whereas identical strings take only n+1 calls. "
     "Therefore n=12 is a demonstrated feasible size, not a hardware maximum; sufficiently long equal strings would eventually be limited by recursive stack depth."),
    ("3. What is gained and lost with two rows?",
     f"TwoRows preserves the exact distance and O(n*m) running time while reducing integer storage from O(n*m) to O(m). "
     f"At n=m=2048 the allocation counters were {number(tr['aux_cells'])} and {number(tw['aux_cells'])} cells, a {memory_ratio:.1f}x reduction. "
     "The discarded rows are unavailable for ordinary backtracking, so this version returns the distance without the full edit script. "
     "Reconstructing a script needs extra computation or a different divide-and-conquer method; two rows also do not guarantee a fixed timing advantage."),
    ("4. Which search would you choose for the city platform?",
     "I would choose KMP for exact pattern search through long city log files because O(n+m) work limits the damage caused by repetitive text and long partial matches. "
     "The LPS setup needs O(m) extra memory and can cost more than Naive on simple inputs. "
     "For the stated mix of potentially long logs, predictable worst-case behavior is more valuable than avoiding that small setup cost. "
     "Typo correction in street names is a separate task: KMP is exact matching, so edit distance is still needed for approximate suggestions.")
]
time_paragraph = (
    f"The measurements contain {len(rows)} positive time samples across {len(groups)} configurations, with two warm-up batches and five recorded batches each. "
    "Times are shown as median milliseconds per invocation, not rounded to whole milliseconds. "
    "Small recursive cases include substantial loop/timer overhead; Java JIT compilation, garbage collection, allocation and CPU activity can also disturb scaling. "
    "The timed algorithms include their counters and, for KMP, preprocessing; generation and CSV writes are excluded. "
    "Consequently the figures describe this instrumented program on this machine, and operation/allocation counters are essential evidence alongside elapsed time."
)
env = {}
for line in (RESULTS / "environment.properties").read_text(encoding="utf-8").splitlines():
    if line and not line.startswith("#") and "=" in line:
        key, value = line.split("=", 1)
        env[key] = value.replace("\\:", ":").replace("\\ ", " ")
environment = (f"{env['os.name']} {env['os.arch']}; {env['java.vm.name']} {env['java.version']}; "
               f"max JVM heap {int(env['max_heap_bytes']) / 1048576:.0f} MiB; "
               f"{env['available_processors']} available processors; seed {env['seed']}.")
test_summary = "308 JUnit 5 tests passed, with zero failures, errors or skips: required reference/edge cases, 100 random edit pairs, 100 random search cases and 100 LPS oracle cases, plus counter/reset checks."
saved_summary = (RESULTS / "test-summary.txt").read_text(encoding="utf-8-sig")
assert re.search(r"Tests run: 308, Failures: 0, Errors: 0, Skipped: 0", saved_summary)
# Derive the saved test count from actual Surefire reports when available.
xml_files = list((ROOT / "target" / "surefire-reports").glob("TEST-*.xml"))
if xml_files:
    totals = {k: 0 for k in ("tests", "failures", "errors", "skipped")}
    for file in xml_files:
        root = ET.parse(file).getroot()
        for k in totals: totals[k] += int(root.attrib[k])
    assert totals == dict(tests=308, failures=0, errors=0, skipped=0), totals

dp_rows = [["Input (n=m=2048)", "Table median (ms)", "TwoRows median (ms)"]]
for kind in ("identical", "random", "disjoint"):
    dp_rows.append([kind, ms(s("Table", kind, 2048)["median_ms"]), ms(s("TwoRows", kind, 2048)["median_ms"])])

md = ["# Assignment 4 - Dynamic Programming and String Algorithms",
      "**Dyussenbay Damir | SE-2523 | 7 November 2026**", "## Experiment Design Card (before the first run)",
      "| Field | Plan |", "|---|---|"]
md.extend(f"| {name} | {text} |" for name, text in card)
md += ["", "The original card and Git history are retained in EXPERIMENT_DESIGN.md.", "## Edit distance: results"]
for name, caption in zip(["01_recursive_calls.png", "02_dp_storage.png"], plot_captions[:2]):
    md += [f"![{caption}](results/plots/{name})", caption]
md += ["| " + " | ".join(dp_rows[0]) + " |", "|---|---:|---:|"]
md += ["| " + " | ".join(row) + " |" for row in dp_rows[1:]]
md += [edit_result, "## String search: results"]
for name, caption in zip(["03_search_comparisons.png", "04_search_time.png"], plot_captions[2:]):
    md += [f"![{caption}](results/plots/{name})", caption]
md += [search_result, "## Short answers"]
for title, text in answers: md += ["### " + title, text]
md += ["## A paragraph on time", time_paragraph, "**Environment:** " + environment,
       "**Validation:** " + test_summary,
       "**Measurement scope:** aux_cells counts algorithm-owned integer cells, not JVM bytes, array headers, output lists or stack memory. Strings and indices use UTF-16 code units.",
       "**AI use and source:** OpenAI Codex assisted with implementation, tests, measured experiments and writing. Requirements and reference values came from the supplied Assignment 4 PDF. Full acknowledgement and reproduction commands are in README.md."]
markdown = re.sub(r"(?<=\|)\n\n(?=\|)", "\n", "\n\n".join(md))
(ROOT / "REPORT.md").write_text(markdown + "\n", encoding="utf-8")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle("BodyA", fontName="Helvetica", fontSize=9.1, leading=12.1, spaceAfter=7, textColor=colors.HexColor("#22323e")))
styles.add(ParagraphStyle("SmallA", parent=styles["BodyA"], fontSize=8.0, leading=10.5, spaceAfter=5))
styles.add(ParagraphStyle("CaptionA", parent=styles["BodyA"], fontSize=8.2, leading=10.8, textColor=colors.HexColor("#36566a"), spaceAfter=9))
styles.add(ParagraphStyle("TitleA", fontName="Helvetica-Bold", fontSize=22, leading=26, textColor=colors.HexColor("#123d56"), spaceAfter=8))
styles.add(ParagraphStyle("SectionA", fontName="Helvetica-Bold", fontSize=15, leading=19, textColor=colors.HexColor("#123d56"), spaceAfter=10))
styles.add(ParagraphStyle("QuestionA", fontName="Helvetica-Bold", fontSize=10, leading=13, spaceBefore=5, spaceAfter=4, textColor=colors.HexColor("#123d56")))

def p(text, style="BodyA"): return Paragraph(escape(text), styles[style])
story = []
def add(text, style="BodyA"): story.append(p(text, style))
def table(data, widths, font_style="SmallA", header=False):
    t = Table([[p(str(cell), font_style) for cell in row] for row in data], colWidths=widths, hAlign="LEFT")
    commands = [("VALIGN", (0,0),(-1,-1),"TOP"), ("LEFTPADDING",(0,0),(-1,-1),7),
                ("RIGHTPADDING",(0,0),(-1,-1),7), ("TOPPADDING",(0,0),(-1,-1),6),
                ("BOTTOMPADDING",(0,0),(-1,-1),5), ("LINEBELOW",(0,0),(-1,-1),0.35,colors.HexColor("#dce3e8")),
                ("ROWBACKGROUNDS",(0,0),(-1,-1),[colors.HexColor("#f2f6f8"),colors.white])]
    if header: commands.append(("BACKGROUND",(0,0),(-1,0),colors.HexColor("#dceaf0")))
    t.setStyle(TableStyle(commands))
    story.append(t)
    story.append(Spacer(1,8))
def plot(name, caption):
    chart_height = 2.9 if name.startswith(("03", "04")) else 2.55
    story.append(Image(str(PLOTS / name), width=491, height=491 * chart_height / 7.1))
    add(caption, "CaptionA")

add("Dynamic Programming & String Algorithms", "TitleA")
add("ASSIGNMENT 4  |  Dyussenbay Damir  |  SE-2523  |  7 November 2026", "SmallA")
add("Experiment Design Card", "SectionA")
table(card, [92,399])
add("Recorded before the first benchmark; the original card and its commit are retained in EXPERIMENT_DESIGN.md. Every algorithm has at least four sizes and every family includes purposeful adverse input.", "SmallA")

story.append(PageBreak())
add("1 / Edit distance", "SectionA")
plot("01_recursive_calls.png", plot_captions[0])
plot("02_dp_storage.png", plot_captions[1])
table(dp_rows, [193,149,149], header=True)
add(edit_result)
add("Storage counts are measured at allocation and exclude Java object overhead and recursion stack. Equal-sized input pairs make n=m here; the implementation supports unequal lengths too.", "SmallA")

story.append(PageBreak())
add("2 / String search", "SectionA")
plot("03_search_comparisons.png", plot_captions[2])
plot("04_search_time.png", plot_captions[3])
add(search_result)
add("Method detail. Each plotted point is the median of five recorded batches. The search timer includes LPS construction on every KMP invocation. Benchmark inputs here contain no complete matches; successful and overlapping matches are checked independently by JUnit.")

story.append(PageBreak())
add("3 / Interpretation and short answers", "SectionA")
for title, text in answers:
    add(title, "QuestionA")
    add(text)
add("A paragraph on time", "QuestionA")
add(time_paragraph)
add("Environment: " + environment, "SmallA")
add("Validation: " + test_summary, "SmallA")
add("AI use and source: OpenAI Codex assisted with the code, tests, measurements and report. The supplied Assignment 4 PDF provided requirements and reference values. README.md contains reproduction commands and the full acknowledgement.", "SmallA")

def decorate(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#d6e1e7"))
    canvas.line(52, 39, A4[0]-52, 39)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#607581"))
    canvas.drawString(52, 25, "DAA / Assignment 4 / Dyussenbay Damir / SE-2523")
    canvas.drawRightString(A4[0]-52, 25, str(doc.page))
    canvas.restoreState()

doc = SimpleDocTemplate(str(ROOT / "REPORT.pdf"), pagesize=A4,
                        leftMargin=52, rightMargin=52, topMargin=42, bottomMargin=49,
                        title="Assignment 4 - Dynamic Programming and String Algorithms",
                        author="Dyussenbay Damir", subject="SE-2523 - measured algorithm experiment")
doc.build(story, onFirstPage=decorate, onLaterPages=decorate)
print(f"Validated {len(rows)} rows, {len(groups)} configurations; generated four plots and REPORT.md / REPORT.pdf")
print("Largest disjoint recursive case:", rd)
print("Largest adversarial search:", na, ka)
