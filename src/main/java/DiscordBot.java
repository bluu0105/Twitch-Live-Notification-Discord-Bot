import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;



public class DiscordBot extends ListenerAdapter { 

	public static void main(String[] args) throws LoginException, InterruptedException{
		
		//environment variable for token
		Dotenv config = Dotenv.load();
		
		//Creates a JDABuilder object to build a JDA object
		final String tokenValue = config.get("BOT_TOKEN");
		JDABuilder builder = JDABuilder.createDefault(tokenValue);
		
		JDA jda = builder.build();
		jda.awaitReady();
		builder.setStatus(OnlineStatus.OFFLINE);
		
		//List containing a single TextChannel for "streams"
		List<TextChannel> channels = jda.getTextChannelsByName("streams", true);
		
		
		//builds a twitchClient twitch4j instance
		TwitchClient twitchClient = TwitchClientBuilder.builder()
				.withEnableHelix(true)
				.withClientId(config.get("ID_CLIENT"))
				.withClientSecret(config.get("SECRET_CLIENT"))
	            .build();

		
				//Creates an ArrayList of gameIDs that are passed into the getStreams method for which games to check
				ArrayList<String> gameIDList = new ArrayList<>();
				gameIDList.add("17828"); //This ID represents: "The Legend of Zelda: Twilight Princess"

		//game_id list: https://raw.githubusercontent.com/Nerothos/TwithGameList/master/game_info.json
		//Loop that uses TwitchAPI to initialize a StreamList, checks relevant streams every minute	
				
		//HashMap streamHash keeps track of whether notified streams are still online
		HashMap<Stream, Boolean> streamHash = new HashMap<>();
		HashMap<Stream, Boolean> updatedStreamHash = new HashMap<>();
		ArrayList<Stream> streamsToRemove = new ArrayList<>();
		
		HashMap<Stream, Boolean> temp = null;

		while(true) {

			System.out.println("Checking for steams now...");
			
			//resultList will update once per specified duration, assigning a new list of currently live channels
			StreamList resultList = twitchClient.getHelix().getStreams(null, null, null, 40, gameIDList, null, null, null).execute();
			
			//This will remove and streams from the streamHash that have gone offline, a new HashMap is created to avoid a ConcurrentModificationException
			for (Stream streamInHash: streamHash.keySet()) {
				if (resultList.getStreams().contains(streamInHash)) {
					updatedStreamHash.put(streamInHash, streamHash.get(streamInHash));
				} else {
					streamsToRemove.add(streamInHash);
				}
			}
			
			//If a stream is new to the resultList it will be set to true to be notified
			for (Stream stream : resultList.getStreams()) {
				if (updatedStreamHash.get(stream) == null) {
					updatedStreamHash.put(stream, true);
				}
			    for(TextChannel channel : channels) {
			    /*	List<UUID> streamTags = stream.getTagIds(); //I couldn't find a way to specify certain tags, will update this later
			    	for(UUID tagID : streamTags) {
			    		System.out.println("Tags from " + stream.getUserName() + ":");
			    		System.out.println(tagID.toString());
			    	} */
			    	
			    	//newly added streams from the resultList will be notified of stream
			    	if (updatedStreamHash.get(stream) == true) {
			    		sendMessage(channel, stream.getUserName() + " is streaming: " + stream.getTitle() + "\nhttps://www.twitch.tv/" + stream.getUserName());
			    		updatedStreamHash.put(stream, false);
			    	}
				}
			}

			//This is for resetting and reassigning the pointers for streamHash and updatedStreamHash, shallow copies are annoying
			for (Stream updatedStreamInHash : updatedStreamHash.keySet()) {
				streamHash.remove(updatedStreamInHash);
			}
			for (Stream key : streamsToRemove) {
				streamHash.remove(key);
			}
			streamsToRemove.clear();
			temp = streamHash;
			streamHash = updatedStreamHash;
			updatedStreamHash = temp;

			TimeUnit.MINUTES.sleep(1);
		}
		

	}
	
	static void sendMessage(TextChannel channel, String message) 
	{
	    channel.sendMessage(message).queue();
	}
	
}
