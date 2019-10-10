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
     * @return Identifier of the entity in the source system.
     */
    String getSourceID();

    /**
     * @return URI of the entity in the source system.
     */
    String getSourceURI();

    /**
     * @return The instance's ID within the sink service. Should not contain
     *         any URI-illegal characters, whether encoded or not, as some sink
     *         services may have problems with them.
     */
    String getSinkID();

    /**
     * @return The ID of the instance's parent within the sink service, or
     *         {@code null} if the entity does not have a parent. Should
     *         not contain any URI-illegal characters, whether encoded or not,
     *         as some sink services may have problems with them.
     */
    String getParentSinkID();

    /**
     * @return The ID of the instance's container within the sink service, or
     *         {@code null} if the entity does not have a container. Should
     *         not contain any URI-illegal characters, whether encoded or not,
     *         as some sink services may have problems with them.
     */
    String getContainerSinkID();

}
