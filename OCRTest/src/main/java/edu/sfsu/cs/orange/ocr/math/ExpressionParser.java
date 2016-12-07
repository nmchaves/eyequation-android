package edu.sfsu.cs.orange.ocr.math;

import com.fathzer.soft.javaluator.DoubleEvaluator;

import java.util.regex.Pattern;

import static android.R.attr.format;

/**
 * Created by nicochaves on 11/27/16.
 */

public class ExpressionParser {

    public static Double parse(String exp) throws Exception {
        exp = preprocessExpression(exp);

        DoubleEvaluator evaluator = new DoubleEvaluator();
        Double result = evaluator.evaluate(exp);
        return result;
    }

    private static String preprocessExpression(String exp) {
        return exp.replace('x', '*');
    }
}
