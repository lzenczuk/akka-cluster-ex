package com.github.lzenczuk.scala.digitalocean

import com.myjeeva.digitalocean.impl.DigitalOceanClient
import com.myjeeva.digitalocean.pojo.{Droplets, Images}

/**
  * Created by dev on 11/11/16.
  */
object DOClientMain extends App{

  // https://github.com/jeevatkm/digitalocean-api-java

  private val client: DigitalOceanClient = new DigitalOceanClient("AUTH_TOKEN_STRING_FROM_DO_GOES_HERE")

  private val availableDroplets: Droplets = client.getAvailableDroplets(0, 10)

  availableDroplets.getDroplets.forEach(droplet => {
    println(s"${droplet.getName}: ${droplet.getStatus}")
    droplet.getNetworks.getVersion4Networks.forEach(network => println(s"${network.getType} ${network.getIpAddress} ${network.getNetmask} ${network.getGateway}"))
  })

  private val availableImages: Images = client.getAvailableImages(0, 10)
  availableImages.getImages.forEach(image => println(s"${image.getId} ${image.getName}"))

}
