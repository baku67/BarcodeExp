package com.example.barcode.features.addItems.manual

data class ManualTypeMeta(
    val code: String,
    val title: String,
    val image: String? = null,
)

data class ManualSubtypeMeta(
    val code: String,
    val parentCode: String,
    val title: String,
    val image: String? = null,
    val goodToKnow: String? = null
)

data class ManualTaxonomy(
    val types: List<ManualTypeMeta>,
    val subtypes: List<ManualSubtypeMeta>
) {
    private val typesByCode = types.associateBy { it.code }
    private val subtypesByParent = subtypes.groupBy { it.parentCode }

    fun typeMeta(type: ManualType): ManualTypeMeta? = typesByCode[type.name]
    fun subtypesOf(type: ManualType): List<ManualSubtypeMeta> = subtypesByParent[type.name].orEmpty()
}
