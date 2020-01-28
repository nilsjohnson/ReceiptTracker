package coffee.nils.dev.receipts.data;

import java.security.PublicKey;

public class ReceiptDBSchema
{
    public static final class ReceiptTable
    {
        public static final String NAME = "receipts";
        public static final int MAX_FIELD_LENGTH_DEFAULT = 255;

        public static final class Cols
        {
            public static final String UUID = "uuid";
            public static final String STORE_NAME = "store_name";
            public static final String DATE = "date";
            public static final String AMOUNT = "amount";
            public static final String IMAGE_IS_CROPPED = "image_is_cropped";
        }
    }

    public static final class StoreNameHashTable
    {
        public static final String NAME = "store_name_hash_table";
        public static final int MAX_FIELD_LENGTH_DEFAULT = 127;

        public static final class COLS
        {
            public static final String KEY = "store_key";
            public static final String VALUE = "store_name_value";
        }
    }
}
