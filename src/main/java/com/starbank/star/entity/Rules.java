package com.starbank.star.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rules")
public class Rules {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_text", nullable = false)
    private String productText;

    @ElementCollection
    @CollectionTable(name = "rule_queries", joinColumns = @JoinColumn(name = "rule_id"))
    private List<RuleQuery> ruleQueries;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductText() {
        return productText;
    }

    public void setProductText(String productText) {
        this.productText = productText;
    }

    public List<RuleQuery> getRuleQueries() {
        return ruleQueries;
    }

    public void setRuleQueries(List<RuleQuery> ruleQueries) {
        this.ruleQueries = ruleQueries;
    }
}