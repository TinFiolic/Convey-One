package one.convey.service;

public interface ScheduledService {

	void sessionTimer();

	void onShutdown();

	void clearAdditionalLists();

}
