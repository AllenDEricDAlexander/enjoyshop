package top.egon.enjoyshop.goods.domain.goods;

/** Server-selected lifecycle actions; never bound from a caller-controlled status. */
public enum ProductActionEnum {
    CREATE, EDIT, AUDIT, PULL, PUT, DELETE, RESTORE, PURGE
}
