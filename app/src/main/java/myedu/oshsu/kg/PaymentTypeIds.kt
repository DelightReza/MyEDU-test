package myedu.oshsu.kg

object PaymentTypeIds {
    const val CONTRACT_TUITION = 1
    const val DIPLOMA_FEE = 4
    const val DIPLOMA_SUPPLEMENT = 9
    const val ACADEMIC_DEBT_PAYMENT = 10

    val TUITION_HISTORY = setOf(CONTRACT_TUITION, DIPLOMA_FEE, DIPLOMA_SUPPLEMENT, ACADEMIC_DEBT_PAYMENT)
}
