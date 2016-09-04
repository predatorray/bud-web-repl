package me.predatorray.bud.web;

import me.predatorray.bud.lisp.builtin.BuiltinsEnvironment;
import me.predatorray.bud.lisp.evaluator.Evaluator;
import me.predatorray.bud.lisp.evaluator.TcoEvaluator;
import me.predatorray.bud.lisp.lang.BudObject;
import me.predatorray.bud.lisp.lexer.Lexer;
import me.predatorray.bud.lisp.parser.Expression;
import me.predatorray.bud.lisp.parser.Parser;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

class EvalJob implements Callable<BudObject> {

    private final Evaluator evaluator;
    private final String source;

    EvalJob(Evaluator evaluator, String source) {
        this.evaluator = evaluator;
        this.source = source;
    }

    @Override
    public BudObject call() throws Exception {
        Lexer lexer = new Lexer(source);
        List<Expression> expressions = new LinkedList<>();
        Parser parser = new Parser(expressions::add);
        parser.feed(lexer.iterator());

        if (expressions.isEmpty()) {
            return null;
        } else {
            return evaluator.evaluateInterruptibly(expressions.get(0), BuiltinsEnvironment.INSTANCE);
        }
    }
}
