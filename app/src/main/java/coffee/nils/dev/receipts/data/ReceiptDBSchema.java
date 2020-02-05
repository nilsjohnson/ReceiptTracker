package coffee.nils.dev.receipts.data;

import java.security.PublicKey;

public class ReceiptDBSchema
{
    public static final class ReceiptTable
    {
        public static final String NAME = "receipts";
        public static final int MAX_FIELD_LENGTH_DEFAULT = 256;

        public static final class COLS
        {
            public static final String UUID = "uuid";
            public static final String STORE_NAME = "store_name";
            public static final String DATE = "date";
            public static final String AMOUNT = "amount";
            public static final String CATEGORY = "category";
            public static final String RECEIPT_BEEN_REVIEWED = "been_reviewed";
        }
    }

    /**
     * for holding <identifier><storeName>
     */
    public static final class StoreNameHashTable
    {
        public static final String NAME = "store_name_hash_table";
        public static final int MAX_FIELD_LENGTH_DEFAULT = 128;

        public static final class COLS
        {
            public static final String KEY = "identifier";
            public static final String VALUE = "store_name";
        }
    }

    /**
     * for holding <storeName><category>
     */
    public static final class CategoryHashTable
    {
        public static final String NAME = "store_category_hash_table";
        public static final int MAX_FIELD_LENGTH_DEFAULT = 128;

        public static final class COLS
        {
            public static final String KEY = "store_name";
            public static final String VALUE = "category";
        }
    }
}