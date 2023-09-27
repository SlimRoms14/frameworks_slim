package slim.provider;

import android.net.Uri;
import android.util.ArraySet;
import android.text.TextUtils;

import com.android.internal.util.ArrayUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class Validators {

    public static interface Validator {
        public boolean validate(String value);
    }

    protected static final Validator BOOLEAN_VALIDATOR =
            new DiscreteValueValidator(new String[] {"0", "1"});

    protected static final Validator NON_NEGATIVE_INTEGER_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String value) {
            try {
                return Integer.parseInt(value) >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };

    protected static final Validator URI_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String value) {
            try {
                Uri.decode(value);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    };

    protected static final Validator COLOR_VALIDATOR =
            new InclusiveIntegerRangeValidator(Integer.MIN_VALUE, Integer.MAX_VALUE);

    protected static final Validator sAlwaysTrueValidator = new Validator() {
        @Override
        public boolean validate(String value) {
            return true;
        }
    };

    protected static final Validator NON_NULL_STRING_VALIDATOR = new Validator() {
        @Override
        public boolean validate(String value) {
            return value != null;
        }
    };

    protected static final class DiscreteValueValidator implements Validator {
        private final String[] mValues;

        public DiscreteValueValidator(String[] values) {
            mValues = values;
        }

        @Override
        public boolean validate(String value) {
            return ArrayUtils.contains(mValues, value);
        }
    }

    protected static final class InclusiveIntegerRangeValidator implements Validator {
        private final int mMin;
        private final int mMax;

        public InclusiveIntegerRangeValidator(int min, int max) {
            mMin = min;
            mMax = max;
        }

        @Override
        public boolean validate(String value) {
            try {
                final int intValue = Integer.parseInt(value);
                return intValue >= mMin && intValue <= mMax;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    protected static final class InclusiveFloatRangeValidator implements Validator {
        private final float mMin;
        private final float mMax;

        public InclusiveFloatRangeValidator(float min, float max) {
            mMin = min;
            mMax = max;
        }

        @Override
        public boolean validate(String value) {
            try {
                final float floatValue = Float.parseFloat(value);
                return floatValue >= mMin && floatValue <= mMax;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    protected static final class DelimitedListValidator implements Validator {
        private final ArraySet<String> mValidValueSet;
        private final String mDelimiter;
        private final boolean mAllowEmptyList;

        public DelimitedListValidator(String[] validValues, String delimiter,
                                      boolean allowEmptyList) {
            mValidValueSet = new ArraySet<String>(Arrays.asList(validValues));
            mDelimiter = delimiter;
            mAllowEmptyList = allowEmptyList;
        }

        @Override
        public boolean validate(String value) {
            ArraySet<String> values = new ArraySet<String>();
            if (!TextUtils.isEmpty(value)) {
                final String[] array = TextUtils.split(value, Pattern.quote(mDelimiter));
                for (String item : array) {
                    if (TextUtils.isEmpty(item)) {
                        continue;
                    }
                    values.add(item);
                }
            }
            if (values.size() > 0) {
                values.removeAll(mValidValueSet);
                // values.size() will be non-zero if it contains any values not in
                // mValidValueSet
                return values.size() == 0;
            } else if (mAllowEmptyList) {
                return true;
            }

            return false;
        }
    }
}
