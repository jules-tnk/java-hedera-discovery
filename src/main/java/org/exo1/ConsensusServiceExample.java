package org.exo1;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsensusServiceExample {
    private static final Logger logger = Logger.getLogger(String.valueOf(ConsensusServiceExample.class));
    private static PrivateKey adminKey;
    private static PrivateKey submitKey;
    private static List<String> topicMessages = new ArrayList<>();

    public static void main(String[] args) {
        try {
            initializeLogging();

            Client client = initializeClient();

            printLogSeparator("Create a topic with adminKey and submitKey");
            TransactionResponse transactionResponse = createTopicWithAdminAndSubmitKey(client);
            TopicId topicId = transactionResponse.getReceipt(client).topicId;
            logger.info("Created new topic " + topicId);
            TopicInfo topicInfo = new TopicInfoQuery().setTopicId(topicId).execute(client);
            System.out.println(topicInfo);

            printLogSeparator("Update topic memo");
            updateTopicMemo(topicId, client);

            printLogSeparator("Subscribe to topic messages");
            subscribeToTopicMessages(topicId, client);

            printLogSeparator("Submit messages to topic");
            publishMessageToTopic(topicId, "My first message", client);
            publishMessageToTopic(topicId, "My second message", client);

            printLogSeparator("Print all topic messages");
            printTopicMessages();

        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e);
        } catch (ReceiptStatusException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printLogSeparator(String message) {
        message = message.toUpperCase();
        System.out.println(
                "\n" + message + "\n===========================================================\n"
        );
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

    private static TransactionResponse createTopicWithAdminAndSubmitKey(Client client) {
        try {
            adminKey = PrivateKey.generate();


            submitKey = PrivateKey.generateED25519();
            PublicKey submitPublicKey = submitKey.getPublicKey();


            logger.info("Creating topic with adminKey and submitKey");
            Transaction<?> transaction = new TopicCreateTransaction()
                    .setTopicMemo("My first topic memo")
                    .setAdminKey(adminKey)
                    .setSubmitKey(submitPublicKey)
                    .freezeWith(client);

            logger.info("Signing ConsensusTopicCreateTransaction with key");
            transaction.sign(adminKey);

            logger.info("Submitting ConsensusTopicCreateTransaction to a Hedera network");
            return transaction.execute(client);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e);
        }
    }

    private static void updateTopicMemo(TopicId topicId, Client client) {
        try {

            Transaction<?> transaction = new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setTopicMemo("Updated demo topic")
                    .freezeWith(client);

            transaction.sign(adminKey);

            TransactionResponse transactionResponse = transaction.execute(client);

            transactionResponse.getReceipt(client);

            logger.info("Updated topic " + topicId);

            TopicInfo topicInfo = new TopicInfoQuery().setTopicId(topicId).execute(client);
            System.out.println(topicInfo);

        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (PrecheckStatusException e) {
            throw new RuntimeException(e);
        } catch (ReceiptStatusException e) {
            throw new RuntimeException(e);
        }
    }



    private static void subscribeToTopicMessages(TopicId topicId, Client client) {
        logger.info("Retrieving topic messages");
        new TopicMessageQuery()
                .setTopicId(topicId)
                .setStartTime(Instant.ofEpochSecond(0))
                .subscribe(client, (resp) -> {
                    String messageAsString = new String(resp.contents, StandardCharsets.UTF_8);
                    logger.info(
                            resp.consensusTimestamp + " received topic message: '" + messageAsString + "'"
                    );
                    topicMessages.add(messageAsString);
                });

    }

    private static void publishMessageToTopic(TopicId topicId, String message, Client client) {
        try {

            logger.info("Submitting message '" + message + "' to topic " + topicId + " with submitKey");

            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage(message)
                    .freezeWith(client)
                    .sign(submitKey)
                    .execute(client)
                    .transactionId
                    .getReceipt(client);

            Thread.sleep(3000);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void printTopicMessages() {
        System.out.println("Topic messages:");
        topicMessages.forEach(System.out::println);
    }

}
