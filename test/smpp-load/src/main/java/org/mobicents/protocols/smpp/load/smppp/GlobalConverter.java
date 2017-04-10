package org.mobicents.protocols.smpp.load.smppp;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.Converter;

public abstract class GlobalConverter implements Converter {

    static final String CMD_SEPARATOR = " ";
    private static final String EXPRESSION_PATTERN = "\\$\\{([^\\$]*)\\}";

    protected GlobalContext ctx;

    public GlobalConverter(GlobalContext ctx) {
        this.ctx = ctx;
    }

    static Pattern expressionPattern = Pattern.compile(EXPRESSION_PATTERN);

    /**
     *
     * @param originalValue
     * @return same value with resolved expressions against context
     */
    protected String resolveExpressions(String originalValue) {
        Matcher matcher = expressionPattern.matcher(originalValue);
        String resolvedString = originalValue.toString();
        while (matcher.find()) {
            try {
                String varName = matcher.group(1);
                String resolvedVar = BeanUtils.getProperty(ctx, varName);
                resolvedString = resolvedString.replace(matcher.group(0), resolvedVar);
            } catch (Exception ex) {
                Logger.getLogger(GlobalConverter.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return resolvedString;
    }

}
