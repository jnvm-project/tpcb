package eu.tsp.transactions;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import static spark.Spark.*;

public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private enum Backend {MAP, MEM, SFS}

    @Option(name = "-backend", usage = "MAP, MEM, SFS")
    private Backend backend = Backend.MAP;

    @Option(name = "-eviction", usage = "max. #objects before eviction")
    private int eviction = 0;

    public static void main(String[] args) {
        new Server().doMain(args);
    }

    public void doMain(String[] args) {

        CmdLineParser parser = new CmdLineParser(this);

        parser.setUsageWidth(80);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        final BankFactory factory = new BankFactory();

        LOG.info("Backend is "+backend);

        Bank b = factory.createBaseBank();
        if (backend.equals(Backend.MEM)) {
            b = factory.createDistributedBank(false,eviction);
        } else if (backend.equals(Backend.SFS)) {
            b = factory.createDistributedBank(true,eviction);
        }

        final Bank bank = b;

        bank.open();

        port(8080);

        get("/:id", (req, res) -> {
            int id = Integer.parseInt(req.params("id"));
            LOG.debug("getBalance("+id+")");
            return Integer.toString(bank.getBalance(id));
        });

        post("/:id", (req, res) -> {
            int id = Integer.parseInt(req.params("id"));
            LOG.debug("createAccount("+id+")");
            bank.createAccount(id);
            return "OK";
        });

        post("/:start/:end", (req, res) -> {
            int start = Integer.parseInt(req.params("start"));
	    int end = Integer.parseInt(req.params("end"));
	    for (int i=start; i<=end; i++) {
		LOG.info("createAccount("+i+")");
		bank.createAccount(i);
	    }
            return "OK";
        });	
	
        put("/:from/:to/:amount", (req,res) -> {
            int from = Integer.parseInt(req.params("from"));
            int to = Integer.parseInt(req.params("to"));
            int amount = Integer.parseInt(req.params("amount"));
            LOG.debug("performTransfer("+from+","+to+","+amount+")");
            bank.performTransfer(from,to,amount);
            return "OK";
        });

        post("/clear/all", (req, res) -> {
            LOG.debug("clear()");
            bank.clear();
            return "OK";
        });

        SignalHandler sh = new SignalHandler() {
            @Override
            public void handle(Signal s) {
                LOG.debug("Shutting down ..");
                bank.close();
                System.exit(0);
                stop();
            }
        };

        Signal.handle(new Signal("INT"), sh);
        Signal.handle(new Signal("TERM"), sh);

        Thread.currentThread().interrupt();
    }

}
