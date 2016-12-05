package edu.sfsu.cs.orange.ocr.math;

import com.fathzer.soft.javaluator.DoubleEvaluator;

/**
 * Created by nicochaves on 11/27/16.
 */

public class ExpressionParser {

    public static Double parse(String exp) throws Exception {
        DoubleEvaluator evaluator = new DoubleEvaluator();
        Double result = evaluator.evaluate(exp);
        return result;
    }
}
