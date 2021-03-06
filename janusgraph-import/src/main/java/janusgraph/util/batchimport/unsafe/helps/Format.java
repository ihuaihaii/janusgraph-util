package janusgraph.util.batchimport.unsafe.helps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Format
{
    /**
     * Default time zone is UTC (+00:00) so that comparing timestamped logs from different
     * sources is an easier task.
     */
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private static final String[] BYTE_SIZES = { "B", "kB", "MB", "GB" };
    private static final String[] COUNT_SIZES = { "", "k", "M", "G", "T" };

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";
    public static final String TIME_FORMAT = "HH:mm:ss.SSS";

    private static final ThreadLocalFormat DATE = new ThreadLocalFormat( DATE_FORMAT );
    private static final ThreadLocalFormat TIME = new ThreadLocalFormat( TIME_FORMAT );
    private static int KB = (int) ByteUnit.kibiBytes( 1 );

    public static String date()
    {
        return date( DEFAULT_TIME_ZONE );
    }

    public static String date( TimeZone timeZone )
    {
        return date( new Date(), timeZone );
    }

    public static String date( long millis )
    {
        return date( millis, DEFAULT_TIME_ZONE );
    }

    public static String date( long millis, TimeZone timeZone )
    {
        return date( new Date( millis ), timeZone );
    }

    public static String date( Date date )
    {
        return date( date, DEFAULT_TIME_ZONE );
    }

    public static String date( Date date, TimeZone timeZone )
    {
        return DATE.format( date, timeZone );
    }

    public static String time()
    {
        return time( DEFAULT_TIME_ZONE );
    }

    public static String time( TimeZone timeZone )
    {
        return time( new Date() );
    }

    public static String time( long millis )
    {
        return time( millis, DEFAULT_TIME_ZONE );
    }

    public static String time( long millis, TimeZone timeZone )
    {
        return time( new Date( millis ), timeZone );
    }

    public static String time( Date date )
    {
        return time( date, DEFAULT_TIME_ZONE );
    }

    public static String time( Date date, TimeZone timeZone )
    {
        return TIME.format( date, timeZone );
    }

    public static String bytes( long bytes )
    {
        return suffixCount( bytes, BYTE_SIZES, KB );
    }

    public static String count( long count )
    {
        return suffixCount( count, COUNT_SIZES, 1_000 );
    }

    private static String suffixCount( long value, String[] sizes, int stride )
    {
        double size = value;
        for ( String suffix : sizes )
        {
            if ( size < stride )
            {
                return String.format( Locale.ROOT, "%.2f %s", Double.valueOf( size ), suffix );
            }
            size /= stride;
        }
        return String.format( Locale.ROOT, "%.2f TB", Double.valueOf( size ) );
    }

    public static String duration( long durationMillis )
    {
        return duration( durationMillis, TimeUnit.DAYS, TimeUnit.MILLISECONDS );
    }

    public static String duration( long durationMillis, TimeUnit highestGranularity, TimeUnit lowestGranularity )
    {
        StringBuilder builder = new StringBuilder();

        TimeUnit[] units = TimeUnit.values();
        reverse( units );
        boolean use = false;
        for ( TimeUnit unit : units )
        {
            if ( unit.equals( highestGranularity ) )
            {
                use = true;
            }

            if ( use )
            {
                durationMillis = extractFromDuration( durationMillis, unit, builder );
                if ( unit.equals( lowestGranularity ) )
                {
                    break;
                }
            }
        }

        return builder.toString();
    }

    private static <T> void reverse( T[] array )
    {
        int half = array.length >> 1;
        for ( int i = 0; i < half; i++ )
        {
            T temp = array[i];
            int highIndex = array.length - 1 - i;
            array[i] = array[highIndex];
            array[highIndex] = temp;
        }
    }

    private static String shortName( TimeUnit unit )
    {
        switch ( unit )
        {
        case NANOSECONDS: return "ns";
        case MICROSECONDS: return "μs";
        case MILLISECONDS: return "ms";
        default: return unit.name().substring( 0, 1 ).toLowerCase();
        }
    }

    private static long extractFromDuration( long durationMillis, TimeUnit unit, StringBuilder target )
    {
        int count = 0;
        long millisPerUnit = unit.toMillis( 1 );
        while ( durationMillis >= millisPerUnit )
        {
            count++;
            durationMillis -= millisPerUnit;
        }
        if ( count > 0 )
        {
            target.append( target.length() > 0 ? " " : "" ).append( count ).append( shortName( unit ) );
        }
        return durationMillis;
    }

    private Format()
    {
        // No instances
    }

    private static class ThreadLocalFormat extends ThreadLocal<DateFormat>
    {
        private final String format;

        ThreadLocalFormat( String format )
        {
            this.format = format;
        }

        String format( Date date, TimeZone timeZone )
        {
            DateFormat dateFormat = get();
            dateFormat.setTimeZone( timeZone );
            return dateFormat.format( date );
        }

        @Override
        protected DateFormat initialValue()
        {
            return new SimpleDateFormat( format );
        }
    }
}
