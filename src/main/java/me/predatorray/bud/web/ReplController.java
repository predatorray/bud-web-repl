package me.predatorray.bud.web;

import me.predatorray.bud.lisp.evaluator.EvaluatingException;
import me.predatorray.bud.lisp.evaluator.Evaluator;
import me.predatorray.bud.lisp.evaluator.TcoEvaluator;
import me.predatorray.bud.lisp.lang.BudObject;
import me.predatorray.bud.lisp.lexer.LexerException;
import me.predatorray.bud.lisp.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;

@Controller
@RequestMapping("repl")
public class ReplController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ExecutorService evaluatingThreads;
    private Evaluator evaluator;

    private long evalTimeout = 5000;

    @PostConstruct
    public void init() {
        this.evaluator = new TcoEvaluator();
        evaluatingThreads = Executors.newCachedThreadPool();

        logger.info("REPL is ready!");
    }

    @PreDestroy
    public void destory() {
        evaluatingThreads.shutdown();
    }

    @RequestMapping(value = "eval", method = RequestMethod.POST)
    public ResponseEntity<String> eval(@RequestBody String source) {
        Future<BudObject> resultFuture = evaluatingThreads.submit(new EvalJob(evaluator, source));
        try {
            BudObject result = resultFuture.get(evalTimeout, TimeUnit.MILLISECONDS);
            return new ResponseEntity<>(result == null ? null : result.toString(), HttpStatus.OK);
        } catch (TimeoutException timeout) {
            resultFuture.cancel(true);
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (ExecutionException executionEx) {
            Throwable cause = executionEx.getCause();
            return dealWithExceptionOfEval(cause);
        } catch (Exception ex) {
            logger.error("ReplController.eval", ex);
            return new ResponseEntity<>("unknown", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<String> dealWithExceptionOfEval(Throwable t) {
        try {
            throw t;
        } catch (LexerException e) {
            return new ResponseEntity<>("[Lexer Error] " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (ParserException e) {
            return new ResponseEntity<>("[Parser Error] " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (EvaluatingException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Error error) {
            throw error;
        } catch (Throwable unknown) {
            logger.error("ReplController.dealWithExceptionOfEval", unknown);
            return new ResponseEntity<>("unknown", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Value("${evalTimeout:5000}")
    public void setEvalTimeout(long evalTimeout) {
        this.evalTimeout = evalTimeout;
    }
}
