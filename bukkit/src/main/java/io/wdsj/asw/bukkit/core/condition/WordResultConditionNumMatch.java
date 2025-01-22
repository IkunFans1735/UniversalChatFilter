package io.wdsj.asw.bukkit.core.condition;

import com.github.houbb.heaven.util.lang.CharUtil;
import com.github.houbb.sensitive.word.api.IWordContext;
import com.github.houbb.sensitive.word.api.IWordResult;
import com.github.houbb.sensitive.word.constant.enums.WordValidModeEnum;
import com.github.houbb.sensitive.word.support.resultcondition.AbstractWordResultCondition;

public class WordResultConditionNumMatch extends AbstractWordResultCondition {
    @Override
    protected boolean doMatch(IWordResult wordResult, String text, WordValidModeEnum modeEnum, IWordContext context) {
        final int startIndex = wordResult.startIndex();
        final int endIndex = wordResult.endIndex();
        if (startIndex > 0) {
            char preC = text.charAt(startIndex - 1);
            if (CharUtil.isDigit(preC)) {
                return false;
            }
        }
        if (endIndex < text.length()) {
            char afterC = text.charAt(endIndex);
            if (CharUtil.isDigit(afterC)) {
                return false;
            }
        }
        for (int i = startIndex; i < endIndex; i++) {
            char c = text.charAt(i);
            if (!CharUtil.isDigit(c)) {
                return true;
            }
        }
        return true;
    }
}

