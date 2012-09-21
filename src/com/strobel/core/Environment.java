package com.strobel.core;

import com.strobel.util.ContractUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author strobelm
 */
public final class Environment {

    private static final Logger logger = Logger.getLogger(Environment.class.getName());

    /**
     * Make sure nobody can instantiate the class
     */
    private Environment() {
        throw ContractUtils.unreachable();
    }

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$([a-zA-Z0-9_]+)", Pattern.COMMENTS);

    /**
     * Get any variable by name if defined on the system
     *
     * @param variable The string with variables to expand. It should be something like '$VARIABLE'
     * @return The expanded variable, empty if arg is null or variable is not defined
     */
    public static String getVariable(final String variable) {
        if (variable == null) {
            return StringUtilities.EMPTY;
        }

        final String expanded = System.getenv(variable);
        return (expanded != null) ? expanded : StringUtilities.EMPTY;
    }

    /**
     * Recursively expands any environment variable(s) defined within a String.
     * If expansion is not possible, the original string will be returned.
     *
     *
     * @param s a string possibly containing one or more environment variables
     * @return The input string with all environment variables expanded
     */
    public static String expandVariables(final String s) {
        return expandVariables(s, true);
    }

    /**
     * Expands any environment variable(s) defined within a String.
     * If expansion is not possible, the original string will be returned.
     *
     * @param s a string possibly containing one or more environment variables
     * @param recursive whether or not variable values should be expanded recursively
     * @return The input string with all environment variables expanded
     */
    public static String expandVariables(final String s, final boolean recursive) {
        final Matcher variableMatcher = VARIABLE_PATTERN.matcher(s);

        StringBuffer expanded = null;
        String variable = null;

        try {
            while (variableMatcher.find()) {
                final int matches = variableMatcher.groupCount();

                // Perform all the variable expansions (if any)
                for (int i = 1; i <= matches; i++) {
                    variable = variableMatcher.group(i);

                    if (expanded == null) {
                        expanded = new StringBuffer();
                    }

                    final String variableValue = getVariable(variable);

                    variableMatcher.appendReplacement(
                        expanded,
                        recursive ? expandVariables(variableValue, true) : variableValue
                    );
                }
            }

            if (expanded != null) {
                variableMatcher.appendTail(expanded);
            }
        }
        catch (Throwable t) {
            logger.log(
                Level.WARNING,
                String.format(
                    "Unable to expand the variable '%s', returning original value: %s",
                    variable,
                    s
                ),
                t
            );
            return s;
        }

        if (expanded != null) {
            return expanded.toString();
        }

        return s;
    }

    public static int getProcessorCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static boolean isSingleProcessor() {
        return getProcessorCount() == 1;
    }
}
