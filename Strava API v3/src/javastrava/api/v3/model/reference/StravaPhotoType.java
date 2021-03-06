package javastrava.api.v3.model.reference;

import javastrava.config.Messages;
import javastrava.config.StravaConfig;
import javastrava.json.impl.gson.serializer.PhotoTypeSerializer;

/**
 * <p>
 * Type of photo - currently Instagram only
 * </p>
 * 
 * @author Dan Shannon
 *
 */
public enum StravaPhotoType {
	/**
	 * Instagram photo
	 */
	INSTAGRAM(StravaConfig.string("StravaPhotoType.instagram"), Messages.string("StravaPhotoType.instagram.description")),  //$NON-NLS-1$ //$NON-NLS-2$
	/**
	 * <p>
	 * Should never occur but may if Strava API behaviour has changed
	 * </p>
	 */
	UNKNOWN(StravaConfig.string("Common.unknown"), Messages.string("Common.unknown.description")); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Identifier
	 */
	private String	id;
	/**
	 * Description
	 */
	private String	description;

	/**
	 * Private constructor used by declarations
	 * @param id Identifier - also used when serialising/deserialising to JSON
	 * @param description Description
	 */
	private StravaPhotoType(final String id, final String description) {
		this.id = id;
		this.description = description;
	}

	/**
	 * Used by JSON serialisation
	 * @return The string representation of this {@link StravaPhotoType} to be used with the Strava API
	 * @see PhotoTypeSerializer#serialize(StravaPhotoType, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
	 */
	public String getValue() {
		return this.id;
	}

	/**
	 * Used by JSON deserialisation
	 * @param id The string represenation of a {@link StravaPhotoType} as returned by the Strava API
	 * @return The matching {@link StravaPhotoType}, or {@link StravaPhotoType#UNKNOWN} if there is no match
	 */
	public static StravaPhotoType create(final String id) {
		StravaPhotoType[] types = StravaPhotoType.values();
		for (StravaPhotoType type : types) {
			if (type.getId().equals(id)) {
				return type;
			}
		}
		return StravaPhotoType.UNKNOWN;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return this.id;
	}
}
