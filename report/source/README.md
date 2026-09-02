# SRS LaTeX 协作说明

## 唯一正文入口

- `tjumain.tex`：报告总入口，通常不需要改。
- `body/srs/index.tex`：SRS 组装文件，只负责引用各章节。
- `body/srs/*.tex`：实际正文。
- `body/srs/functions/*.tex`：按角色和模块拆分的功能需求。

旧版单文件 SRS、DOCX 转换脚本和模板示例均放在 `archive/`，仅用于回溯，不参与编译。

SRS 一级结构严格参照《UML 大战需求分析》附录 1：

1. `01_introduction.tex`：简介。
2. `02_goals_stakeholders_scope.tex`：目标、涉众分析和范围。
3. `03_business_concept_analysis.tex`：业务概念分析。
4. `04_business_process_analysis.tex`：业务流程分析。
5. `05_functional_requirements.tex` 与 `functions/`：功能性需求。
6. `06_nonfunctional_requirements.tex`：非功能性需求。
7. `07_appendices.tex`：附录。
8. `08_revision_history.tex`：版本修订记录。

## 编译

```bash
cd report/source
make pdf
```

产物固定写入 `report/result/tjumain.pdf`。编译中间文件位于 `report/source/build/`，不提交。

模板优先使用 macOS 自带中文字体；在其他系统会自动回退到 TeX Live 的 Fandol 字体，组员无需手工改字体配置。

## 图片协作

所有 SRS 图片放在 `figures/srs/`，文件名和分工见该目录的 `README.md`。正文使用 `\srsfigure`：

```tex
\srsfigure{figures/srs/01-system-context.pdf}{系统上下文图}{fig:system-context}
```

文件未存在时会自动生成带预期路径的占位框；补图后编号、标题和交叉引用自动更新。优先提交 PlantUML/Mermaid/Draw.io 源文件和导出的 PDF/PNG。

## 并行修改规则

1. 一个 PR 主要修改一类章节，不同时格式化全库。
2. 新增功能先确定稳定需求编号，再补接口、数据和测试映射。
3. P0/P1 属于基线验收范围；P2 明确为可选增强，不作为基线验收失败条件。
4. 提交前必须运行 `make pdf`，检查无 LaTeX 错误、无断引用、无越界表格。
5. 交叉验收版将 `setup/info.tex` 中的 `\srsanonymousfalse` 改为 `\srsanonymoustrue`，同时匿名封面和 PDF 元数据。
