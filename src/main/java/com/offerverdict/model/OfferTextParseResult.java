package com.offerverdict.model;

import java.util.ArrayList;
import java.util.List;

public class OfferTextParseResult {
    private String analysisMode;
    private OfferRiskDraft draft;
    private boolean parsed;
    private String summary;
    private String sourceLabel;
    private String parseWarning;
    private List<String> extractedFields = new ArrayList<>();
    private List<String> missingCriticalFields = new ArrayList<>();
    private List<String> evidenceSnippets = new ArrayList<>();

    public OfferRiskDraft getDraft() {
        return draft;
    }

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public void setDraft(OfferRiskDraft draft) {
        this.draft = draft;
    }

    public boolean isParsed() {
        return parsed;
    }

    public void setParsed(boolean parsed) {
        this.parsed = parsed;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getParseWarning() {
        return parseWarning;
    }

    public void setParseWarning(String parseWarning) {
        this.parseWarning = parseWarning;
    }

    public List<String> getExtractedFields() {
        return extractedFields;
    }

    public void setExtractedFields(List<String> extractedFields) {
        this.extractedFields = extractedFields;
    }

    public List<String> getMissingCriticalFields() {
        return missingCriticalFields;
    }

    public void setMissingCriticalFields(List<String> missingCriticalFields) {
        this.missingCriticalFields = missingCriticalFields;
    }

    public List<String> getEvidenceSnippets() {
        return evidenceSnippets;
    }

    public void setEvidenceSnippets(List<String> evidenceSnippets) {
        this.evidenceSnippets = evidenceSnippets;
    }
}
