package org.exo2;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import io.github.cdimascio.dotenv.Dotenv;
import org.exo1.ConsensusServiceExample;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenServiceExample {
    private static final Logger logger = Logger.getLogger(String.valueOf(ConsensusServiceExample.class));

    private static PrivateKey treasuryKey;
    private static PrivateKey adminKey;
    private static PrivateKey feeScheduleKey;

    public static void main(String[] args) {
        initializeLogging();

        Client client = initializeClient();
    }

    private static void initializeLogging() {
        try {
            FileHandler fh = new FileHandler("./hederaExample.log");
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
            logger.config("Logging initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Client initializeClient() {
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);
        client.setDefaultMaxTransactionFee(new Hbar(100));
        client.setDefaultMaxQueryPayment(new Hbar(50));
        logger.info("Client initialized");
        return client;
    }

}
