package org.exo2;

import com.hedera.hashgraph.sdk.*;

import io.github.cdimascio.dotenv.Dotenv;
import org.exo1.ConsensusServiceExample;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenServiceExample {
    private static final Logger logger = Logger.getLogger(String.valueOf(ConsensusServiceExample.class));

    private static PrivateKey adminKey;

    private static PrivateKey supplyKey;

    private static PrivateKey feeScheduleKey;

    private static PrivateKey treasuryKey;

    private static AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));

    public static void main(String[] args) {
        initializeLogging();

        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        Client client = initializeClient();

        // Create account A and B
        AccountId accountAId = AccountId.fromString(Dotenv.load().get("ACCOUNT_A_ID"));
        AccountId accountBId = AccountId.fromString(Dotenv.load().get("ACCOUNT_B_ID"));

        // Create token
        TokenId tokenId = createToken(client);

        // Get token info
        showTokenInfo(tokenId, client);

        // Update token memo
        updateTokenMemo(tokenId, "My new memo", client);

        // Get token info
        showTokenInfo(tokenId, client);

        // update token custom fee
        List<CustomFee> newCustomFee = List.of(
                new CustomRoyaltyFee()
                        .setFeeCollectorAccountId(myAccountId)
                        .setNumerator(1)
                        .setDenominator(10));
        updateTokenCustomFees(tokenId, newCustomFee, client);

        // Get token info
        showTokenInfo(tokenId, client);

        // Mint token
        TransactionReceipt mintTransaction1 = mintNFT(tokenId, client);
        TransactionReceipt mintTransaction2 = mintNFT(tokenId, client);

        // Get token info
        showTokenInfo(tokenId, client);

        System.out.println("BEFORE TRANSFER");
        System.out.println("My account balance:");
        showAccountBalance(myAccountId, client);
        System.out.println("Account A balance:");
        showAccountBalance(accountAId, client);
        System.out.println("Account B balance:");
        showAccountBalance(accountBId, client);

        transferNFT(tokenId, mintTransaction1, client.getOperatorAccountId(), accountBId, client);

        System.out.println("AFTER TRANSFER");
        System.out.println("My account balance:");
        showAccountBalance(myAccountId, client);
        System.out.println("Account A balance:");
        showAccountBalance(accountAId, client);
        System.out.println("Account B balance:");
        showAccountBalance(accountBId, client);

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

            return newAccountId;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TokenId createToken(Client client) {
        adminKey = PrivateKey.generateED25519();

        supplyKey = PrivateKey.generateED25519();

        feeScheduleKey = PrivateKey.generateED25519();

        treasuryKey = PrivateKey.generateED25519();

        System.out.println("Creating token...");
        try {

            TokenCreateTransaction tokenCreateTransaction = new TokenCreateTransaction()
                    .setTokenName("JujuNFT")
                    .setTokenSymbol("JNFT")
                    .setTokenMemo("Memo of JujuNFT")
                    .setDecimals(0)
                    .setInitialSupply(0)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setAdminKey(adminKey)
                    .setSupplyKey(supplyKey)
                    .setFeeScheduleKey(feeScheduleKey)
                    .setTreasuryAccountId(myAccountId)
                    .setCustomFees(
                            List.of(
                                    new CustomRoyaltyFee()
                                            .setNumerator(1)
                                            .setDenominator(20)
                                            .setFeeCollectorAccountId(client.getOperatorAccountId())))
                    .freezeWith(client);

            TokenCreateTransaction signedTokenCreateTransaction = tokenCreateTransaction.sign(adminKey)
                    .sign(treasuryKey);

            TokenId tokenId = signedTokenCreateTransaction.execute(client).getReceipt(client).tokenId;
            System.out.println("Token created with token ID: " + tokenId);
            return tokenId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private static void showTokenInfo(TokenId tokenId, Client client) {
        System.out.println("Trying to get token info...");
        try {
            TokenInfo tokenInfo = new TokenInfoQuery()
                    .setTokenId(tokenId)
                    .execute(client);
            System.out.println(tokenInfo);

        } catch (TimeoutException | PrecheckStatusException e) {
            e.printStackTrace();
        }
    }

    private static void updateTokenMemo(TokenId tokenId, String newMemo, Client client) {
        try {
            System.out.println("Trying to update token memo...");
            TokenUpdateTransaction tokenUpdateTransaction = new TokenUpdateTransaction()
                    .setTokenId(tokenId)
                    .setTokenMemo(newMemo)
                    .freezeWith(client);

            tokenUpdateTransaction.sign(adminKey).execute(client);
            System.out.println("Token memo updated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateTokenCustomFees(TokenId tokenId, List<CustomFee> customFees, Client client) {
        System.out.println("Trying to update token custom fees...");
        try {

            TokenFeeScheduleUpdateTransaction transaction = new TokenFeeScheduleUpdateTransaction()
                    .setTokenId(tokenId)
                    .setCustomFees(customFees)
                    .freezeWith(client);

            transaction.sign(feeScheduleKey).sign(adminKey).execute(client);
            System.out.println("Token custom fees updated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Token custom fees updated.");
    }

    private static TransactionReceipt mintNFT(TokenId tokenId, Client client) {
        System.out.println("Trying to mint NFT...");
        try {
            TokenMintTransaction transaction = new TokenMintTransaction()
                    .setTokenId(tokenId)
                    .addMetadata("meta 1".getBytes())
                    .addMetadata("meta 2".getBytes())
                    .addMetadata("meta 3".getBytes())
                    .freezeWith(client);

            TransactionResponse transactionResponse = transaction.sign(supplyKey).execute(client);
            TransactionReceipt transactionReceipt = transactionResponse.getReceipt(client);
            System.out.println("NFT minted.");

            return transactionReceipt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void transferNFT(TokenId tokenId, TransactionReceipt mintTransaction1,
            AccountId sender, AccountId receiver, Client client) {
        System.out.println("Trying to transfer NFT...");
        try {
            TransferTransaction transaction = new TransferTransaction()
                    .addTokenTransfer(tokenId, sender, -1)
                    .addTokenTransfer(tokenId, receiver, 1)
                    .freezeWith(client);

            TransactionResponse transactionResponse = transaction.sign(supplyKey).execute(client);
            TransactionReceipt transactionReceipt = transactionResponse.getReceipt(client);
            System.out.println("NFT transferred.");

        } catch (Exception e) {
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
