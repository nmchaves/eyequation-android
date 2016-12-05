package edu.sfsu.cs.orange.ocr;

/**
 * Created by nicochaves on 12/4/16.
 */

public class EquationResult {

    // Number to identify which equation was solved
    private int equationNumber;

    // Whether or not the equation was successfully solved
    private boolean success;

    // If success is false, then this describes the error
    private String errorMessage;

    // The solution
    private Double solution;

    public EquationResult(int equationNumber, Double solution, boolean success, String errorMessage) {
        this.equationNumber = equationNumber;
        this.success = success;
        this.errorMessage = errorMessage;
        this.solution = solution;
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
