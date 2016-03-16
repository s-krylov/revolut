package com.revolut.model

case class AccountTransfer (id: Int,
                            clientAccountNumber: String,
                            clientAccountBic: String,
                            recipientAccountNumber: String,
                            recipientAccountBic: String,
                            amount: BigDecimal) {

}
