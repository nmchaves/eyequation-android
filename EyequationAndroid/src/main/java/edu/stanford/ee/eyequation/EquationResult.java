package edu.stanford.ee.eyequation;

public class EquationResult {

    // Number to identify which equation was solved
    private int equationNumber;

    // The text of the equation that was OCR'd
    private String ocrText;

    // Whether or not the equation was successfully solved
    private boolean success;

    // If success is false, then this describes the error
    private String errorMessage;

    // The solution
    private Double solution;

    public EquationResult(int equationNumber, String ocrText, Double solution, boolean success, String errorMessage) {
        this.equationNumber = equationNumber;
        this.ocrText = ocrText;
        this.success = success;
        this.errorMessage = errorMessage;
        this.solution = solution;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public int getEquationNumber() {
        return equationNumber;
    }

    public void setEquationNumber(int equationNumber) {
        this.equationNumber = equationNumber;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Double getSolution() {
        return solution;
    }

    public void setSolution(Double solution) {
        this.solution = solution;
    }
}
