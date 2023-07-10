package org.example;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.concurrent.TimeoutException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HederaExamples {
    private static final Logger logger = Logger.getLogger(String.valueOf(HederaExamples.class));

    public static void main(String[] args) {
        initializeLogging();

        Client client = initializeClient();

        AccountId newClientAccountId = createNewAccount(client);

        sendHbar(client, client.getOperatorAccountId(), newClientAccountId);

        Hbar queryCost = getAccountBalanceQueryCost(client, newClientAccountId);

        AccountBalance newAccountBalance = getAccountBalance(client, newClientAccountId);
    }

    private static Client initializeClient() {
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);
        client.setDefaultMaxTransactionFee(new Hbar(100));
        client.setDefaultMaxQueryPayment(new Hbar(50));
        logger.info("Client initialized");
        // get client info
        showAccountInfo(client.getOperatorAccountId(), client);

        return client;
    }

    private static void showAccountInfo(AccountId accountId, Client client) {
        System.out.println("Getting account info from account ID: " + accountId);
        try {
            AccountInfo accountInfo = new AccountInfoQuery()
                    .setAccountId(accountId)
                    .execute(client);

            System.out.println(accountInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static AccountId createNewAccount(Client client) {
        System.out.println();
        // Generate a new key pair
        PrivateKey newAccountPrivateKey = PrivateKey.generateED25519();
        PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();
        logger.info("Private key = " + newAccountPrivateKey);
        logger.info("Public key = " + newAccountPublicKey);
        try {
            logger.info("Trying to create a new account...");

            TransactionResponse newAccount = new AccountCreateTransaction()
                    .setKey(newAccountPublicKey)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .execute(client);

            // Get the new account ID
            AccountId newAccountId = newAccount.getReceipt(client).accountId;

            // Log the account ID
            logger.info("The new account ID is: " + newAccountId);

            // Get the new account's balance
            AccountBalance accountBalanceQuery = new AccountBalanceQuery()
                    .setAccountId(newAccountId)
                    .execute(client);

            // Log the balance
            logger.info("The new account balance is: " + accountBalanceQuery.hbars);
            return newAccountId;

        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e);
        } catch (ReceiptStatusException e) {
            throw new RuntimeException(e);
        }
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

    private static void sendHbar(Client client, AccountId senderAccountId, AccountId receiverAccountId) {
        logger.info("Trying to send 1000 tinybar from " + senderAccountId + " to " + receiverAccountId);
        try {
            TransactionResponse sendHbar = new TransferTransaction()
                    .addHbarTransfer(senderAccountId, Hbar.fromTinybars(-1000)) // Sending account
                    .addHbarTransfer(receiverAccountId, Hbar.fromTinybars(1000)) // Receiving account
                    .execute(client);
            logger.info("Sent 1000 tinybar from " + senderAccountId + " to " + receiverAccountId);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e);
        }
    }

    private static Hbar getAccountBalanceQueryCost(Client client, AccountId accountId) {
        try {
            Hbar queryCost = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .getCost(client);
            logger.info("Query cost: " + queryCost);
            return queryCost;
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private static AccountBalance getAccountBalance(Client client, AccountId accountId) {
        try {
            AccountBalance accountBalance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(client);
            logger.info("Account balance: " + accountBalance);
            return accountBalance;
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

}
