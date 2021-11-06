require('isomorphic-fetch');

const RPM = 50
let rpmCounter = RPM

setInterval(async () => {

 
  for(let i=0; i<RPM; i++){
    try{
      const response = await fetch('http://localhost:8081/articles?delayInMilliseconds=1000',
      {
        method: "OPTIONS"
      })
      const data = await response.text()
      console.log(data)
      rpmCounter++
    }catch(e){
      console.error(e)
      rpmCounter++
    }
  }

}, 1000)
