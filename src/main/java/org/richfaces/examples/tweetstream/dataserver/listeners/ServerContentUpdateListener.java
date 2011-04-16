package org.richfaces.examples.tweetstream.dataserver.listeners;

import org.jboss.jbw2011.keynote.demo.model.Tweet;
import org.jboss.jbw2011.keynote.demo.model.TweetAggregate;
import org.jboss.logging.Logger;
import org.richfaces.examples.tweetstream.dataserver.jms.PublishController;
import org.richfaces.examples.tweetstream.dataserver.service.TweetStreamPersistenceService ;
import org.richfaces.examples.tweetstream.dataserver.util.TweetAggregateConverter;
import org.richfaces.examples.tweetstream.domain.TwitterAggregate;

import javax.ejb.*;
import javax.inject.Inject;
import java.util.List;

/**
 * Listens/polls for data updates from the server's persistenceServiceBean.
 * When an update is needed it retrieves the updated content and publishes
 * to the JMS topic using the PublishController.
 * <p/>
 * The current impl polls, for updates, but will be updated to
 * listen for update events at some point.
 *
 * //TODO change name of class to ServerContentListenerBean
 *
 * @author <a href="mailto:jbalunas@redhat.com">Jay Balunas</a>
 */
@Singleton
public class ServerContentUpdateListener implements ServerContentListener {
  private static final String INTERVAL = "10";
  private static final String EVERY = "*";

  @Inject
  Logger log;

  @Inject
  private TweetStreamPersistenceService persistenceService;

  @Inject
  PublishController pubControl;

  private Tweet lastTweet = null;

  @Override
  public void startServerListener() {

    //TODO Status the timer for the push.
    //  This should not call pollServer right away
    //  If it is called too early the you could get
    //  Service Tracker has not been initialized error

    //this.pollServer();

  }

  //TODO Fix this so it does not start polling right away
  @Schedule(dayOfMonth = EVERY, month = EVERY, year = EVERY, second = INTERVAL, minute = EVERY, hour = EVERY)
  @Timeout
  private void pollServer(){
    log.debug("ServerContentListener polling triggered");

    //check if updates have been made
    if (updateContentAvailable()) {

      //Fetch the updates
      TweetAggregate svrAggregate = persistenceService.getAggregate();

      //Convert to local domain model
      TwitterAggregate twitterAggregate = TweetAggregateConverter.convertTwitterAggregate(svrAggregate);

      //Send push controller updated content to publish

      //pubControl.publishView(twitterAggregate);

    }

    log.debug("ServerContentListener polling completed");
  }

  private boolean updateContentAvailable() {
    //Get the latest tweet from the service
    List<Tweet> curTweetList = persistenceService.allTweetsSortedByTime(1);
    Tweet currentTweet = curTweetList.isEmpty() ? null : curTweetList.get(0);

    if ((lastTweet == null) || (currentTweet == null)) {
      log.debug("First check for new server content, or content still empty from server");
      lastTweet = currentTweet;
      return true;
    } else if (lastTweet.getTimestamp() != currentTweet.getTimestamp()) {
      log.debug("Server content has been updated, content update required");
      lastTweet = currentTweet;
      return true;
    } else {
      log.debug("Server content has not been updated, no content update required");
      return false;
    }
  }

}