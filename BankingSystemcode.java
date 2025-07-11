package bankingsystemsimulation;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BankingSystemSimulation {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private Bank<Account> bank;

    public BankingSystemSimulation() {
        bank = new Bank<>();
        showLoginScreen();
    }

    private void showLoginScreen() {
        String[] options = {"Admin Login", "User Login"};
        int choice = JOptionPane.showOptionDialog(null, "Select Login Type", "Banking System Login",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            if (authenticateAdmin()) {
                new AdminDashboard(bank);
            } else {
                JOptionPane.showMessageDialog(null, "Access Denied.");
            }
        } else if (choice == 1) {
            String accountIdStr = JOptionPane.showInputDialog("Enter Account ID:");
            JPasswordField passwordField = new JPasswordField();
            int confirm = JOptionPane.showConfirmDialog(null, passwordField, "Enter Password", JOptionPane.OK_CANCEL_OPTION);

            if (accountIdStr != null && confirm == JOptionPane.OK_OPTION) {
                try {
                    int accountId = Integer.parseInt(accountIdStr.trim());
                    String password = new String(passwordField.getPassword()).trim();
                    Account account = bank.getAccount(accountId);
                    if (account.getPassword().equals(password)) {
                        new UserDashboard(account, bank);
                    } else {
                        JOptionPane.showMessageDialog(null, "Incorrect password.");
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Invalid Account ID format.");
                } catch (AccountNotFoundException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage());
                }
            }
        }
    }

    private boolean authenticateAdmin() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int option = JOptionPane.showConfirmDialog(null, panel, "Admin Authentication", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            return username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD);
        }
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BankingSystemSimulation::new);
    }
}

// AdminDashboard Class
class AdminDashboard {
    public AdminDashboard(Bank<Account> bank) {
        JFrame frame = new JFrame("Admin Dashboard");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JButton showAccountsButton = new JButton("Show All Accounts");
        JButton addAccountButton = new JButton("Add Account");
        JButton deleteAccountButton = new JButton("Delete Account");
        JButton simulateTransactionsButton = new JButton("Simulate Transactions");
        JButton logoutButton = new JButton("Logout");

        showAccountsButton.addActionListener(e -> {
            outputArea.setText("All Accounts:\n");
            for (Account account : bank.getAllAccounts()) {
                outputArea.append(account.toString() + "\n");
            }
        });

        addAccountButton.addActionListener(e -> {
            JTextField idField = new JTextField();
            JTextField balanceField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            JPanel panel = new JPanel(new GridLayout(3, 2));
            panel.add(new JLabel("Account ID:"));
            panel.add(idField);
            panel.add(new JLabel("Initial Balance:"));
            panel.add(balanceField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);

            int result = JOptionPane.showConfirmDialog(null, panel, "Add Account", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int id = Integer.parseInt(idField.getText().trim());
                    double balance = Double.parseDouble(balanceField.getText().trim());
                    String password = new String(passwordField.getPassword()).trim();
                    bank.addAccount(new Account(id, balance, password));
                    bank.saveAccounts();
                    outputArea.append("Account added successfully.\n");
                } catch (NumberFormatException ex) {
                    outputArea.append("Invalid input.\n");
                } catch (IllegalArgumentException ex) {
                    outputArea.append(ex.getMessage() + "\n");
                }
            }
        });

        deleteAccountButton.addActionListener(e -> {
            String accountIdStr = JOptionPane.showInputDialog("Enter Account ID to delete:");
            if (accountIdStr != null) {
                try {
                    int accountId = Integer.parseInt(accountIdStr.trim());
                    bank.deleteAccount(accountId);
                    bank.saveAccounts();
                    outputArea.append("Account deleted successfully.\n");
                } catch (NumberFormatException ex) {
                    outputArea.append("Invalid Account ID format.\n");
                } catch (AccountNotFoundException ex) {
                    outputArea.append(ex.getMessage() + "\n");
                }
            }
        });

        simulateTransactionsButton.addActionListener(e -> {
            Thread transactionThread = new Thread(() -> {
                for (Account account : bank.getAllAccounts()) {
                    // Simulate some transactions
                    new TransactionThread(account, TransactionType.DEPOSIT, 100, outputArea).start();
                    new TransactionThread(account, TransactionType.WITHDRAW, 50, outputArea).start();
                }
            });
            transactionThread.start();
            outputArea.setText("Simulating transactions...\n");
        });

        logoutButton.addActionListener(e -> {
            frame.dispose();
            new BankingSystemSimulation();
        });

        JPanel panel = new JPanel(new GridLayout(5, 1));
        panel.add(showAccountsButton);
        panel.add(addAccountButton);
        panel.add(deleteAccountButton);
        panel.add(simulateTransactionsButton);
        panel.add(logoutButton);

        frame.add(panel, BorderLayout.WEST);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}

// UserDashboard Class
class UserDashboard {
    public UserDashboard(Account account, Bank<Account> bank) {
        JFrame frame = new JFrame("User Dashboard");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JButton depositButton = new JButton("Deposit");
        JButton withdrawButton = new JButton("Withdraw");
        JButton showBalanceButton = new JButton("Show Balance");
        JButton logoutButton = new JButton("Logout");

        depositButton.addActionListener(e -> {
            String amountStr = JOptionPane.showInputDialog("Enter amount to deposit:");
            try {
                double amount = Double.parseDouble(amountStr);
                account.deposit(amount);
                bank.saveAccounts(); // Save changes
                outputArea.setText("Deposited: " + amount + "\nNew Balance: " + account.getBalance() + "\n");
            } catch (NumberFormatException ex) {
                outputArea.append("Invalid amount.\n");
            }
        });

        withdrawButton.addActionListener(e -> {
            String amountStr = JOptionPane.showInputDialog("Enter amount to withdraw:");
            try {
                double amount = Double.parseDouble(amountStr);
                account.withdraw(amount);
                bank.saveAccounts(); // Save changes
                outputArea.setText("Withdrew: " + amount + "\nNew Balance: " + account.getBalance() + "\n");
            } catch (NumberFormatException ex) {
                outputArea.append("Invalid amount.\n");
            } catch (InsufficientFundsException ex) {
                outputArea.append(ex.getMessage() + "\n");
            }
        });

        showBalanceButton.addActionListener(e -> outputArea.setText("Current Balance: " + account.getBalance() + "\n"));

        logoutButton.addActionListener(e -> {
            frame.dispose();
            new BankingSystemSimulation();
        });

        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.add(depositButton);
        panel.add(withdrawButton);
        panel.add(showBalanceButton);
        panel.add(logoutButton);

        frame.add(panel, BorderLayout.WEST);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}

// Bank Class
class Bank<T extends Account> {
    private List<T> accounts;
    private static final String DATA_FILE = "accounts.dat";

    public Bank() {
        accounts = loadAccounts();
    }

    public List<T> getAllAccounts() {
        return accounts;
    }

    public void saveAccounts() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(accounts);
        } catch (IOException e) {
            System.err.println("Error saving accounts: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> loadAccounts() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<T>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading accounts: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public T getAccount(int accountId) throws AccountNotFoundException {
        return accounts.stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    public void addAccount(T account) {
        if (accounts.stream().anyMatch(a -> a.getId() == account.getId())) {
            throw new IllegalArgumentException("Account ID already exists");
        }
        accounts.add(account);
    }

    public void deleteAccount(int accountId) throws AccountNotFoundException {
        T account = getAccount(accountId);
        accounts.remove(account);
    }
}


// Account Class
class Account implements Serializable {
    private int id;
    private double balance;
    private String password;

    public Account(int id, double balance, String password) {
        this.id = id;
        this.balance = balance;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public double getBalance() {
        return balance;
    }

    public String getPassword() {
        return password;
    }

    public synchronized void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public synchronized void withdraw(double amount) throws InsufficientFundsException {
        if (amount > balance) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        balance -= amount;
    }

    @Override
    public String toString() {
        return "Account ID: " + id + ", Balance: " + balance;
    }
}

// InsufficientFundsException Class
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

// AccountNotFoundException Class
class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

// TransactionThread Class with Deadlock
class TransactionThread extends Thread {
    private final Account account;
    private final TransactionType type;
    private final double amount;
    private final JTextArea outputArea;
    private final Object lock1 = new Object(); // Additional lock
    private final Object lock2 = new Object(); // Another lock

    public TransactionThread(Account account, TransactionType type, double amount, JTextArea outputArea) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.outputArea = outputArea;
    }

    @Override
    public void run() {
        // Simulate deadlock using interdependent locks
        if (type == TransactionType.DEPOSIT) {
            synchronized (lock1) {
                synchronized (lock2) {
                    try {
                        account.deposit(amount);
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("Deposited: " + amount + " to Account ID " + account.getId() + "\n");
                        });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("Error: " + e.getMessage() + " for Account ID " + account.getId() + "\n");
                        });
                    }
                }
            }
        } else if (type == TransactionType.WITHDRAW) {
            synchronized (lock2) {
                synchronized (lock1) {
                    try {
                        account.withdraw(amount);
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("Withdrew: " + amount + " from Account ID " + account.getId() + "\n");
                        });
                    } catch (InsufficientFundsException e) {
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append("Error: " + e.getMessage() + " for Account ID " + account.getId() + "\n");
                        });
                    }
                }
            }
        }
    }
}

enum TransactionType {
    DEPOSIT,
    WITHDRAW
}