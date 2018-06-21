package edu.illinois.library.metaslurper.entity;

/**
 * "Abstract" base interface.
 *
 * @author Alex Dolski UIUC
 */
public interface Entity {

    /**
     * @return Source service key.
     */
    String getServiceKey();

    /**
     * @return The instance's ID within the sink service. Should not contain
     *         any URI-illegal characters, whether encoded or not, as some sink
     *         services may have problems with them.
     */
    String getSinkID();

    /**
     * @return Identifier of the entity in the source system.
     */
    String getSourceID();

    /**
     * @return URI of the entity in the source system.
     */
    String getSourceURI();

}
