package io.wdsj.asw.bukkit.method;

import java.util.*;

public class WildCardLineResolver {
    public List<String> resolveWildCardLine(String line) {
        String[] split = line.split("\\|");
        if (split.length == 1) {
            return Collections.singletonList(line);
        }

        List<List<String>> options = new ArrayList<>();
        for (String part : split) {
            String[] alternatives = part.split("\\*");
            options.add(Arrays.asList(alternatives));
        }

        List<String> result = new ArrayList<>();
        combine(options, 0, "", result);
        return result;
    }

    private void combine(List<List<String>> options, int index, String current, List<String> result) {
        if (index == options.size()) {
            result.add(current);
            return;
        }
        for (String option : options.get(index)) {
            combine(options, index + 1, current + option, result);
        }
    }
}