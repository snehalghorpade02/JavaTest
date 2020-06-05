package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.FundTransferRequest;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.InSufficientBalanceException;
import com.db.awmd.challenge.service.NotificationService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();
  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsRepositoryInMemory(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
        "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) {
    return accounts.get(accountId);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }

  @Override
  public void transferFunds(FundTransferRequest fundTransferRequest) throws InvalidAmountException,InSufficientBalanceException{
    BigDecimal amountToTransfer = fundTransferRequest.getAmount();
    String accountFromId = fundTransferRequest.getAccountFrom();
    String accountToId = fundTransferRequest.getAccountTo();

    if(amountToTransfer.compareTo(BigDecimal.ZERO) < 0){
      throw new InvalidAmountException(
              "Transfer amount should always be a positive number");
      }
    if(checkBalanceForTransfer(accountFromId,amountToTransfer)){
      BigDecimal fromAccountBalance = accounts.get(accountFromId).getBalance();
      fromAccountBalance = fromAccountBalance.subtract(amountToTransfer);
      accounts.get(accountFromId).setBalance(fromAccountBalance);

      //Notify FromAccount
      String transferDescription = "Your Account " + accountFromId + "has been debited with amount" + amountToTransfer + "and transfer to account " + accountToId;
      notificationService.notifyAboutTransfer(accounts.get(accountFromId),transferDescription);

      BigDecimal toAccountBalance = accounts.get(accountToId).getBalance();
      toAccountBalance = toAccountBalance.add(amountToTransfer);
      accounts.get(accountToId).setBalance(toAccountBalance);

      //Notify ToAccount
      transferDescription = "Your Account " + accountToId + "has been credited with amount" + amountToTransfer + " from account " + accountFromId;
      notificationService.notifyAboutTransfer(accounts.get(accountToId),transferDescription);
    }
    else
    {
      throw  new InSufficientBalanceException(
              "Insufficient Balance in From Account to transfer the amount");
    }
  }

  private boolean checkBalanceForTransfer(String accountFromId, BigDecimal amount ){
    boolean result = false;
    BigDecimal balance = accounts.get(accountFromId).getBalance();
    if(balance.compareTo(amount) >= 0) {
      result = true;
    }
    else
    {
      result = false;
    }
    return result;
  }
}
