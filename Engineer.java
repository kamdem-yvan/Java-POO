package strms.interfaces;

/**
 * Interface for classes capable of generating reports.
 */
public interface Reportable {
    /**
     * Generates a formatted report as a String.
     * @return the report content
     */
    String generateReport();
}
