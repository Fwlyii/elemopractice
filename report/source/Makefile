MAIN := tjumain
BUILD_DIR := build
RESULT_DIR := ../result
RESULT := $(RESULT_DIR)/$(MAIN).pdf

.PHONY: pdf clean

pdf:
	@mkdir -p $(BUILD_DIR) $(RESULT_DIR)
	latexmk -xelatex -interaction=nonstopmode -halt-on-error -file-line-error -outdir=$(BUILD_DIR) $(MAIN).tex
	cp $(BUILD_DIR)/$(MAIN).pdf $(RESULT)
	@echo "PDF: $(RESULT)"

clean:
	latexmk -C -outdir=$(BUILD_DIR) $(MAIN).tex
