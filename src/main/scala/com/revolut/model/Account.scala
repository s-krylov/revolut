package com.revolut.model

case class Account (id: Int,
                    accountNumber: String,
                    bic: String,
                    var amount: BigDecimal) {}
