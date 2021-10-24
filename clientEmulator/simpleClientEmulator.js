require('isomorphic-fetch');

const RPM = 5000
let rpmCounter = RPM

setInterval(async () => {

 
  for(let i=0; i<RPM; i++){
    try{
      const response = await fetch('http://localhost:8080/api')
      const data = await response.text()
      console.log(data)
      rpmCounter++
    }catch(e){
      console.error(e)
      rpmCounter++
    }
  }

}, 1000)
