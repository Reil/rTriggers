package com.reil.bukkit.rTriggers.persistence;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 *
 * @author Sammy
 */
@Entity()
@Table(name = "rTriggerLimits")
public class TriggerLimit {

    @Id
    private int id;
    @NotNull
    private String playerName;
    @NotEmpty
    private String message;
    @NotNull
    long time;

    public TriggerLimit(){}
    
    public TriggerLimit(String playerName, String message, long time){
    	this.playerName = playerName;
    	this.message = message;
    	this.time = time;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String ply) {
        this.playerName = ply;
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(playerName);
    }

    public void setPlayer(Player player) {
        this.playerName = player.getName();
    }  
    public long getTime(){
    	return this.time;
    }
    
    public void setTime(long time) {
    	this.time = time;
    }
}