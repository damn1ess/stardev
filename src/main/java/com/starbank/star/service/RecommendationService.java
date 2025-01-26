package com.starbank.star.service;

import com.starbank.star.DTO.RecommendationDTO;
import com.starbank.star.entity.Rules;
import com.starbank.star.entity.RuleQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal; // Используем класс BigDecimal для большей точности операций
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private final JdbcTemplate jdbcTemplate;
    private final RulesService rulesService;

    @Autowired
    public RecommendationService(JdbcTemplate jdbcTemplate, RulesService rulesService) {
        this.jdbcTemplate = jdbcTemplate;
        this.rulesService = rulesService;
    }

    public List<RecommendationDTO> getRecommendations(UUID userId) {
        List<Rules> rulesList = rulesService.getAllRules(); // Загружаем список правил с помощью метода из RulesService
        return rulesList.stream()
                .filter(rule -> applyRule(rule, userId))
                .map(rule -> new RecommendationDTO(rule.getProductName(), rule.getId().toString(), rule.getProductText()))
                .collect(Collectors.toList()); // Добавляем рекомендацию в случае соблюдения правил
    }

    private boolean evaluateQuery(RuleQuery query, UUID userId) {
        return switch (query.getQueryType()) {
            case "USER_OF" -> isUserOf(userId, query.getArguments().get(0));
            case "ACTIVE_USER_OF" -> isActiveUserOf(userId, query.getArguments().get(0));
            case "TRANSACTION_SUM_COMPARE" -> isTransactionSumCompare(userId, query.getArguments());
            case "TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW" ->
                    isTransactionSumCompareDepositWithdraw(userId, query.getArguments());
            default -> throw new IllegalArgumentException("Wrong argument: " + query.getQueryType());};
    }

    private boolean applyRule(Rules rule, UUID userId) {
        boolean allConditionsMet = true;

        for (RuleQuery query : rule.getRuleQueries()) {
            boolean result = evaluateQuery(query, userId);
            if (query.isNegate()) {
                result = !result;
            }
            allConditionsMet = allConditionsMet && result; // Сравниваем все полученные результаты
        }

        return allConditionsMet; // Возвращаем true только если все условия выполнены
    }

    private boolean isUserOf(UUID userId, String productType) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND product_type = ?";
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{userId, productType}, Integer.class);
        return count != null && count > 0; // Возвращаем true, если есть хотя бы одна транзакция данного типа
    }

    private boolean isActiveUserOf(UUID userId, String productType) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND product_type = ?";
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{userId, productType}, Integer.class);
        return count != null && count >= 5; // Возвращаем true, если есть хотя бы 5 транзакций данного типа
    }

    private boolean isTransactionSumCompare(UUID userId, List<String> arguments) {
        String accountType = arguments.get(0); // Тип счета
        String comparisonOperator = arguments.get(2); // Оператор сравнения
        BigDecimal threshold = new BigDecimal(arguments.get(3)); // Пороговое значение

        BigDecimal transactionSum = getTransactionSum(userId, accountType); // Получаем сумму транзакций данного пользователя

        switch (comparisonOperator) {
            case ">":
                return transactionSum.compareTo(threshold) > 0;
            case "<":
                return transactionSum.compareTo(threshold) < 0;
            case "=":
                return transactionSum.compareTo(threshold) == 0;
            case ">=":
                return transactionSum.compareTo(threshold) >= 0;
            case "<=":
                return transactionSum.compareTo(threshold) <= 0;
            default:
                throw new IllegalArgumentException("Wrong argument: " + comparisonOperator);
        }
    }

    private boolean isTransactionSumCompareDepositWithdraw(UUID userId, List<String> arguments) {
        String accountType = arguments.get(0); // Тип счета
        String comparisonOperator = arguments.get(1); // Оператор сравнения

        BigDecimal depositSum = getDepositSum(userId, accountType);
        BigDecimal withdrawSum = getWithdrawSum(userId, accountType);

        switch (comparisonOperator) {
            case ">":
                return depositSum.compareTo(withdrawSum) > 0;
            case "<":
                return depositSum.compareTo(withdrawSum) < 0;
            case "=":
                return depositSum.compareTo(withdrawSum) == 0;
            case ">=":
                return depositSum.compareTo(withdrawSum) >= 0;
            case "<=":
                return depositSum.compareTo(withdrawSum) <= 0;
            default:
                throw new IllegalArgumentException("Wrong argument: " + comparisonOperator);
        }
    }

    private BigDecimal getTransactionSum(UUID userId, String accountType) {
        String sql = "SELECT SUM(amount) FROM transactions WHERE user_id = ? AND product_type = ? GROUP BY user_id";
        return jdbcTemplate.queryForObject(sql, new Object[]{userId, accountType}, BigDecimal.class);
    }

    private BigDecimal getDepositSum(UUID userId, String accountType) {
        String sql = "SELECT SUM(amount) FROM transactions WHERE user_id = ? AND product_type = ? AND type = 'DEPOSIT' GROUP BY user_id";
        return jdbcTemplate.queryForObject(sql, new Object[]{userId, accountType}, BigDecimal.class);
    }

    private BigDecimal getWithdrawSum(UUID userId, String accountType) {
        String sql = "SELECT SUM(amount) FROM transactions WHERE user_id = ? AND product_type = ? AND type = 'WITHDRAW' GROUP BY user_id";
        return jdbcTemplate.queryForObject(sql, new Object[]{userId, accountType}, BigDecimal.class);
    }
}