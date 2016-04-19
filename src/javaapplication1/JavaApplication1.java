package javaapplication1;
/**
 *
 * @author tramtran
 */
import java.io.*;
import java.util.*;

public class JavaApplication1 {

    private static byte EXPRESSION_MAX_LENGTH = 54;
    private static byte ONE_EXPRESSION_LENGTH = 6;
    private static byte NONE = 0;

    public static void main(String[] args) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String input = bufferedReader.readLine();
            String[] parts = input.split(" ");
            if (parts.length > 2 && parts.length < 7) {
                long[] numbers = new long[parts.length - 1];
                long target = Long.parseLong(parts[0]);
                for (int i = 0; i < numbers.length; i++) {
                    numbers[i] = Long.parseLong(parts[i + 1]);
                }
                long exp = getExpression(target, numbers);
                printExpression(exp, numbers, target);
            }
        } catch (NumberFormatException ex) {
            System.out.println("Please input integers!");
        } catch (IOException e) {
        }
    }

    //An expression has 64 bits but only 59 bits are used.
    //54 rightmost bits are reserved to remember the arithmetic expression using postfix notation.
    //Each operand or operator in the arithmetic expression takes up 6 bits. 
    //The rightmost bit in the 6-bit indicates whether it is an operand or operator. 1 for operand and 0 for operator.
    //the rest is used as a mask to keep track of the integers that have been used.
    public static long getExpression(long target, long[] numbers) {
        //make sure target is not bigger than the maximum value that given numbers can produce
        if (!isInRange(target, numbers)) {
            return NONE;
        }
        int numbersCount = numbers.length;
        double[] givenNumbers = new double[numbersCount];
        for (int i = 0; i < numbersCount; i++) {
            givenNumbers[i] = numbers[i];
        }
        double targetValue = target;
        //to make sure that all possible combinations have been reached
        //we combine each value and expression that have been reached to the values and expressions that have not been processed
        HashMap<Double, List<Long>> solvedValues = new HashMap<>();//values and expressions have been reached
        Queue<Holder> remainder = new LinkedList<>();//values and expressions have not been processed yet

        //Initialize
        for (int i = 0; i < numbersCount; i++) {
            List<Long> l = new ArrayList<>();
            long numExpression = (1L << i);
            long expression = ((numExpression << EXPRESSION_MAX_LENGTH) | (numExpression << 1 | 1));
            l.add(expression);
            solvedValues.put(givenNumbers[i], l);
            Holder temp = new Holder(givenNumbers[i], expression);
            remainder.add(temp);
        }
        loop:
        while (!remainder.isEmpty()) {

            Holder holder = remainder.poll();
            long curExpression = holder.expression;
            int curMask = generateMaskFromExpression(curExpression);//help keep track of the integers that are in the expression
            double curValue = holder.value;
            ArrayList<Holder> solvedValuesCopy = new ArrayList<>();
            for (Double key : solvedValues.keySet()) {
                List<Long> list = solvedValues.get(key);
                for (long exp : list) {
                    solvedValuesCopy.add(new Holder(key, exp));
                }
            }
            for (int i = 0; i < solvedValuesCopy.size(); i++) {
                Holder curSolvedValue = solvedValuesCopy.get(i);
                long expression = curSolvedValue.expression;
                int mask = generateMaskFromExpression(expression);
                double value = curSolvedValue.value;
                if ((mask & curMask) == 0) {//guarantee no integer is used twice 
                    for (int operator = 0; operator < 6; operator++) {
                        double newValue = 0;
                        long operatorExp = 1 << (operator + 1);
                        switch (operator) {
                            case 0:
                                newValue = curValue + value;
                                break;
                            case 1:
                                newValue = curValue - value;
                                break;
                            case 2:
                                newValue = value - curValue;
                                break;
                            case 3:
                                newValue = curValue * value;
                                break;
                            case 4:
                                if (value == 0) {
                                    continue;
                                }
                                newValue = curValue / value;
                                break;
                            case 5:
                                if (curValue == 0) {
                                    continue;
                                }
                                newValue = value / curValue;
                                operatorExp = ((1L << 1) | 1) << 1;
                                break;
                        }
                        long newExpression = 0;
                        //arithmetic expression using postfix notation
                        if (operator == 2 | operator == 5) {
                            newExpression = appendExpression(expression, curExpression);
                        } else {
                            newExpression = appendExpression(curExpression, expression);
                        }
                        newExpression = appendExpression(newExpression, operatorExp, ONE_EXPRESSION_LENGTH);
                        int newMask = generateMaskFromExpression(newExpression);
                        List<Long> l = solvedValues.get(newValue);
                        if (l == null) {
                            l = new ArrayList<>();
                        }
                        boolean isNewEntry = true;
                        for (long exp : l) {
                            int m = generateMaskFromExpression(exp);
                            if (m == newMask) {
                                isNewEntry = false;
                                break;
                            }
                        }
                        if (isNewEntry) {
                            if (newValue == targetValue && usedAllNumbers(newMask, numbersCount)) {
                                return newExpression;
                            }
                            l.add(newExpression);
                            solvedValues.put(newValue, l);
                            Holder temp = new Holder(newValue, newExpression);
                            remainder.add(temp);
                        }
                    }
                }
            }
        }
        return NONE;
    }

    private static boolean isInRange(long target, long[] givenNumbers) {
        long checker = 1;
        for (int i = 0; i < givenNumbers.length; i++) {

            long v = Math.abs(givenNumbers[i]);
            if (v == 1 | v == 0) {
                return true;
            }
            try {
                checker = Math.multiplyExact(checker, v);
            } catch (ArithmeticException e) {
                return true;
            }

        }
        return checker >= Math.abs(target);
    }

    private static int generateMaskFromExpression(long expression) {
        int ret = (int) (expression >> EXPRESSION_MAX_LENGTH);
        return ret;
    }

    private static boolean usedAllNumbers(int mask, int length) {
        int checker = (1 << length) - 1;
        return mask == checker;
    }

    private static long appendExpression(long toExp, long exp) {
        long mask = generateMaskFromExpression(exp);
        long toMask = generateMaskFromExpression(toExp);
        long newMask = mask | toMask;
        toExp = toExp & ((1L << EXPRESSION_MAX_LENGTH) - 1);//only operands
        exp = exp & ((1L << EXPRESSION_MAX_LENGTH) - 1);
        long temp = exp;
        while (temp != 0) {
            toExp = toExp << ONE_EXPRESSION_LENGTH;
            temp = temp >> ONE_EXPRESSION_LENGTH;
        }
        toExp = toExp | exp;
        return toExp | (newMask << EXPRESSION_MAX_LENGTH);
    }

    private static long appendExpression(long toExp, long exp, int length) {
        long mask = generateMaskFromExpression(exp);
        long toMask = generateMaskFromExpression(toExp);
        long newMask = mask | toMask;
        toExp = (toExp & ((1L << EXPRESSION_MAX_LENGTH) - 1)) << length;
        exp = exp & ((1L << EXPRESSION_MAX_LENGTH) - 1);
        toExp = toExp | exp;
        return toExp | (newMask << EXPRESSION_MAX_LENGTH);
    }

    private static void printExpression(long exp, long[] givenNumbers, long targetValue) {
        if (exp == 0) {
            System.out.println("none");
            return;
        }
        long temp = exp;
        int count = 9;
        Stack<String> st = new Stack<>();
        while (--count >= 0) {
            long checker = (1 << ONE_EXPRESSION_LENGTH) - 1;
            int value = (int) ((temp >> ONE_EXPRESSION_LENGTH * count) & checker);
            if (value == 0) {
                continue;
            }
            if (isNumber(value)) {
                int c = 0;
                while (((value = value >> 1) & 1) == 0) {
                    c++;
                }
                st.push(String.valueOf(givenNumbers[c]));
            } else {
                int c = 0;
                while (((value = value >> 1) & 1) == 0) {
                    c++;
                }
                if (c == 0 && ((value >> 1) & 1) != 0) {
                    c = 5;
                }

                String s = "";
                String b = st.pop();
                String a = st.pop();
                switch (c) {
                    case 0:
                        s = a + "+" + b;
                        break;
                    case 1:
                        s = a + "-" + b;
                        break;
                    case 2:
                        s = a + "-" + b;
                        break;
                    case 3:
                        s = a + "*" + b;
                        break;
                    case 4:

                        s = a + "/" + b;
                        break;
                    case 5:

                        s = a + "/" + b;
                        break;
                }
                st.push("(" + s + ")");
            }
        }
        String expr = st.pop();

        System.out.println(expr + " = " + targetValue);
    }

    private static boolean isNumber(int value) {
        return (value & 1) == 1;
    }

    private static class Holder {

        double value;
        long expression;

        Holder(double value, long expression) {
            this.value = value;
            this.expression = expression;
        }

        Holder() {

        }
    }
}
