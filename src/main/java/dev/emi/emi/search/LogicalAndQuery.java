package dev.emi.emi.search;

import dev.emi.emi.api.stack.EmiStack;

import java.util.List;

public class LogicalAndQuery extends Query {
    private final List<Query> queries;

    public LogicalAndQuery(List<Query> queries) {
        this.queries = queries;
    }

    @Override
    public boolean matches(EmiStack stack) {
        for (int i = 0; i < queries.size(); i++) {
            Query q = queries.get(i);
            boolean failure = q.negated;
            if (q.matches(stack) == failure) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matchesUnbaked(EmiStack stack) {
        for (int i = 0; i < queries.size(); i++) {
            Query q = queries.get(i);
            boolean failure = q.negated;
            if (q.matchesUnbaked(stack) == failure) {
                return false;
            }
        }
        return true;
    }
}
