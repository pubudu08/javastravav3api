package javastrava.api.v3.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.model.StravaAthlete;
import javastrava.api.v3.model.StravaSegmentEffort;
import javastrava.api.v3.model.StravaStatistics;
import javastrava.api.v3.model.reference.StravaGender;
import javastrava.api.v3.model.reference.StravaResourceState;
import javastrava.api.v3.service.AthleteService;
import javastrava.api.v3.service.exception.NotFoundException;
import javastrava.api.v3.service.exception.UnauthorizedException;
import javastrava.cache.StravaCache;
import javastrava.cache.impl.StravaCacheImpl;
import javastrava.util.Paging;
import javastrava.util.PagingHandler;

/**
 * <p>
 * Implementation of {@link AthleteService}
 * </p>
 *
 * @author Dan Shannon
 *
 */
public class AthleteServiceImpl extends StravaServiceImpl implements AthleteService {
	/**
	 * <p>
	 * Returns an instance of {@link AthleteService athlete services}
	 * </p>
	 *
	 * <p>
	 * Instances are cached so that if 2 requests are made for the same token,
	 * the same instance is returned
	 * </p>
	 *
	 * @param token
	 *            The Strava access token to be used in requests to the Strava
	 *            API
	 * @return An instance of the athlete services
	 */
	public static AthleteService instance(final Token token) {
		// Get the service from the token's cache
		AthleteService service = token.getService(AthleteService.class);

		// If it's not already there, create a new one and put it in the token
		if (service == null) {
			service = new AthleteServiceImpl(token);
			token.addService(AthleteService.class, service);
		}
		return service;
	}

	/**
	 * Cache of athletes
	 */
	private final StravaCache<StravaAthlete, Integer> athleteCache;

	/**
	 * Cache of segment efforts
	 */
	private final StravaCache<StravaSegmentEffort, Long> effortCache;

	/**
	 * <p>
	 * Private constructor requires a valid token to instantiate, see {@link AthleteServiceImpl#instance}
	 * </p>
	 * @param token A valid access token returned by Strava's OAuth process
	 */
	private AthleteServiceImpl(final Token token) {
		super(token);
		this.athleteCache = new StravaCacheImpl<StravaAthlete, Integer>(StravaAthlete.class, token);
		this.effortCache = new StravaCacheImpl<StravaSegmentEffort, Long>(StravaSegmentEffort.class, token);
	}

	/**
	 * @see javastrava.api.v3.service.StravaService#clearCache()
	 */
	@Override
	public void clearCache() {
		this.athleteCache.removeAll();
		this.effortCache.removeAll();
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#getAthlete(java.lang.Integer)
	 */
	@Override
	public StravaAthlete getAthlete(final Integer id) {
		// Attempt to get the athlete from the cache
		StravaAthlete athlete = this.athleteCache.get(id);
		if ((athlete != null) && (athlete.getResourceState() != StravaResourceState.META)) {
			return athlete;
		}

		// Attempt to get the athlete from the API if it's not in cache
		try {
			athlete = this.api.getAthlete(id);
		} catch (final NotFoundException e) {
			return null;
		} catch (final UnauthorizedException e) {
			if (accessTokenIsValid()) {
				athlete = new StravaAthlete();
				athlete.setId(id);
				return athlete;
			}
			throw e;
		}

		// Put the athlete in the cache
		this.athleteCache.put(athlete);
		return athlete;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#getAthleteAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<StravaAthlete> getAthleteAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return getAthlete(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#getAuthenticatedAthlete()
	 */
	@Override
	public StravaAthlete getAuthenticatedAthlete() {
		// Try to get it from the cache
		StravaAthlete athlete = this.athleteCache.get(this.getToken().getAthlete().getId());
		if (athlete != null) {
			return athlete;
		}

		// Now get it via the API
		athlete = this.api.getAuthenticatedAthlete();

		// Put it in the cache and return
		this.athleteCache.put(athlete);
		return athlete;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#getAuthenticatedAthleteAsync()
	 */
	@Override
	public CompletableFuture<StravaAthlete> getAuthenticatedAthleteAsync() {
		return StravaServiceImpl.future(() -> {
			return getAuthenticatedAthlete();
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAthleteFriends(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listAllAthleteFriends(final Integer athleteId) {
		// Always get from Strava, not from cache, as there's no way to be sure the cache is up to date
		final List<StravaAthlete> athletes = PagingHandler.handleListAll(thisPage -> listAthleteFriends(athleteId, thisPage));

		// Return them
		return athletes;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAthleteFriendsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAllAthleteFriendsAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return listAllAthleteFriends(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAthleteKOMs(java.lang.Integer)
	 */
	@Override
	public List<StravaSegmentEffort> listAllAthleteKOMs(final Integer athleteId) {
		return PagingHandler.handleListAll(thisPage -> listAthleteKOMs(athleteId, thisPage));
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAthleteKOMsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaSegmentEffort>> listAllAthleteKOMsAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return listAllAthleteKOMs(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAthletesBothFollowing(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listAllAthletesBothFollowing(final Integer athleteId) {
		return PagingHandler.handleListAll(thisPage -> listAthletesBothFollowing(athleteId, thisPage));

	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAthletesBothFollowingAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAllAthletesBothFollowingAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return listAllAthletesBothFollowing(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAuthenticatedAthleteFriends()
	 */
	@Override
	public List<StravaAthlete> listAllAuthenticatedAthleteFriends() {
		return PagingHandler.handleListAll(thisPage -> listAuthenticatedAthleteFriends(thisPage));
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAllAuthenticatedAthleteFriendsAsync()
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAllAuthenticatedAthleteFriendsAsync() {
		return StravaServiceImpl.future(() -> {
			return listAllAuthenticatedAthleteFriends();
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteFriends(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listAthleteFriends(final Integer id) {
		return listAthleteFriends(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteFriends(Integer,
	 *      Paging)
	 */
	@Override
	public List<StravaAthlete> listAthleteFriends(final Integer id, final Paging pagingInstruction) {
		final List<StravaAthlete> athletes = PagingHandler.handlePaging(pagingInstruction, thisPage -> Arrays.asList(AthleteServiceImpl.this.api
				.listAthleteFriends(id, thisPage.getPage(), thisPage.getPageSize())));

		// Put them in the cache so they can be read back later
		this.athleteCache.putAll(athletes);

		// Return them
		return athletes;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteFriendsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAthleteFriendsAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return listAthleteFriends(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteFriendsAsync(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAthleteFriendsAsync(final Integer athleteId, final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listAthleteFriends(athleteId, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteKOMs(java.lang.Integer)
	 */
	@Override
	public List<StravaSegmentEffort> listAthleteKOMs(final Integer id) {
		return listAthleteKOMs(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteKOMs(Integer,
	 *      Paging)
	 */
	@Override
	public List<StravaSegmentEffort> listAthleteKOMs(final Integer id, final Paging pagingInstruction) {
		final List<StravaSegmentEffort> efforts = PagingHandler.handlePaging(
				pagingInstruction,
				thisPage -> Arrays.asList(AthleteServiceImpl.this.api.listAthleteKOMs(id, thisPage.getPage(),
						thisPage.getPageSize())));

		this.effortCache.putAll(efforts);

		return efforts;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteKOMsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaSegmentEffort>> listAthleteKOMsAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return listAthleteKOMs(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthleteKOMsAsync(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaSegmentEffort>> listAthleteKOMsAsync(final Integer athleteId, final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listAthleteKOMs(athleteId, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthletesBothFollowing(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listAthletesBothFollowing(final Integer id) {
		return listAthletesBothFollowing(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthletesBothFollowing(Integer,
	 *      Paging)
	 */
	@Override
	public List<StravaAthlete> listAthletesBothFollowing(final Integer id, final Paging pagingInstruction) {
		final List<StravaAthlete> athletes = PagingHandler.handlePaging(pagingInstruction, thisPage -> Arrays.asList(AthleteServiceImpl.this.api
				.listAthletesBothFollowing(id, thisPage.getPage(), thisPage.getPageSize())));

		this.athleteCache.putAll(athletes);

		return athletes;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthletesBothFollowingAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAthletesBothFollowingAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return listAthletesBothFollowing(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAthletesBothFollowingAsync(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAthletesBothFollowingAsync(final Integer athleteId, final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listAthletesBothFollowing(athleteId, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAuthenticatedAthleteFriends()
	 */
	@Override
	public List<StravaAthlete> listAuthenticatedAthleteFriends() {
		return listAuthenticatedAthleteFriends(null);
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAuthenticatedAthleteFriends(Paging)
	 */
	@Override
	public List<StravaAthlete> listAuthenticatedAthleteFriends(final Paging pagingInstruction) {
		final List<StravaAthlete> athletes = PagingHandler.handlePaging(
				pagingInstruction,
				thisPage -> Arrays.asList(AthleteServiceImpl.this.api.listAuthenticatedAthleteFriends(
						thisPage.getPage(), thisPage.getPageSize())));

		this.athleteCache.putAll(athletes);

		return athletes;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAuthenticatedAthleteFriendsAsync()
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAuthenticatedAthleteFriendsAsync() {
		return StravaServiceImpl.future(() -> {
			return listAuthenticatedAthleteFriends();
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#listAuthenticatedAthleteFriendsAsync(javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAuthenticatedAthleteFriendsAsync(final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listAuthenticatedAthleteFriends(pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#statistics(Integer)
	 */
	@Override
	public StravaStatistics statistics(final Integer id) {
		try {
			return this.api.statistics(id);
		} catch (final NotFoundException e) {
			return null;
		} catch (final UnauthorizedException e) {
			if (accessTokenIsValid()) {
				return new StravaStatistics();
			}
			throw e;
		}
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#statisticsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<StravaStatistics> statisticsAsync(final Integer athleteId) {
		return StravaServiceImpl.future(() -> {
			return statistics(athleteId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#updateAuthenticatedAthlete(java.lang.String,
	 *      java.lang.String, java.lang.String,
	 *      javastrava.api.v3.model.reference.StravaGender, java.lang.Float)
	 */
	@Override
	public StravaAthlete updateAuthenticatedAthlete(final String city, final String state, final String country,
			final StravaGender sex, final Float weight) {
		final StravaAthlete athlete = this.api.updateAuthenticatedAthlete(city, state, country, sex, weight);

		this.athleteCache.put(athlete);

		return athlete;
	}

	/**
	 * @see javastrava.api.v3.service.AthleteService#updateAuthenticatedAthleteAsync(java.lang.String, java.lang.String, java.lang.String, javastrava.api.v3.model.reference.StravaGender, java.lang.Float)
	 */
	@Override
	public CompletableFuture<StravaAthlete> updateAuthenticatedAthleteAsync(final String city, final String state, final String country, final StravaGender sex, final Float weight) {
		return StravaServiceImpl.future(() -> {
			return updateAuthenticatedAthlete(city, state, country, sex, weight);
		});
	}

}
