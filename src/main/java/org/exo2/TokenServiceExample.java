package org.exo2;

import com.hedera.hashgraph.sdk.*;

import io.github.cdimascio.dotenv.Dotenv;
import org.exo1.ConsensusServiceExample;

import java.util.List;
import java.util.concurrent.TimeoutException;
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

        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        Client client = initializeClient();

        // Create account A and B
        AccountId accountAId = createAccount(client);
        AccountId accountBId = createAccount(client);

        // Create token
        TokenId tokenId = createToken(client);

        // Get token info
        showTokenInfo(tokenId, client);

        // Update token memo
        updateTokenMemo(tokenId, "My new memo", client);

        // Get token info
        showTokenInfo(tokenId, client);

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

    private static AccountId createAccount(Client client) {
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

    private static TokenId createToken(Client client) {
        PrivateKey adminKey = PrivateKey.generateED25519();
        PublicKey adminPublicKey = adminKey.getPublicKey();
        PrivateKey supplyKey = PrivateKey.generateED25519();
        PublicKey supplyPublicKey = supplyKey.getPublicKey();
        PrivateKey feeScheduleKey = PrivateKey.generateED25519();
        PublicKey feeSchedulePublicKey = feeScheduleKey.getPublicKey();
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));

        try {

            TransactionResponse tokenCreateTransaction = new TokenCreateTransaction()
                    .setTokenName("JujuNFT")
                    .setTokenSymbol("JNFT")
                    .setDecimals(0)
                    .setInitialSupply(0)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setAdminKey(adminPublicKey)
                    .setSupplyKey(supplyPublicKey)
                    .setFeeScheduleKey(feeSchedulePublicKey)
                    .setTreasuryAccountId(myAccountId)
                    .freezeWith(client)
                    .execute(client);

            TokenId tokenId = tokenCreateTransaction.getReceipt(client).tokenId;
            return tokenId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private static void showTokenInfo(TokenId tokenId, Client client) {
        try {
            TokenInfo tokenInfo = new TokenInfoQuery()
                    .setTokenId(tokenId)
                    .execute(client);

            System.out.println("Token name: " + tokenInfo.name);
            System.out.println("Token symbol: " + tokenInfo.symbol);
            System.out.println("Token admin key: " + tokenInfo.adminKey);
            System.out.println("Token supply key: " + tokenInfo.supplyKey);
            System.out.println("Token fee schedule key: " + tokenInfo.feeScheduleKey);
            System.out.println("Token custom fees: " + tokenInfo.customFees);
        } catch (TimeoutException | PrecheckStatusException e) {
            e.printStackTrace();
        }
    }

    private static void updateTokenMemo(TokenId tokenId, String newMemo, Client client) {
        try {

            TokenUpdateTransaction tokenUpdateTransaction = new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMemo(newMemo);

            tokenUpdateTransaction.execute(client);

        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (PrecheckStatusException e) {
            e.printStackTrace();
        }
    }

    private static void showAccountBalance(AccountId accountId, Client client) {
        try {
            AccountBalance accountBalance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(client);
            System.out.println("Account balance: " + accountBalance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
