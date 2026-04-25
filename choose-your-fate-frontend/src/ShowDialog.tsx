import { scene } from "./types";

export function ShowDialog(nextscene: scene, changeScene: Function) {
  return (
    <div className="App">
        <img src={require("./" + nextscene.img)} alt='Not available' style={{ width: "100vw", height: "100vh", objectFit: "contain" }} />
        <div className="dialogChoiceContainer">
          {nextscene.choices.map((choice) => (
                    <div className='choice' title={choice.name} onClick={() => changeScene(choice.destination_id)}> 
                      {choice.name}
                    </div>
                  ))}
        </div>
        <div className='dialogContainer'>
            <div className='dialogTextContainer'>
            {[...nextscene.dialog]}
            </div>
            <br />
            {/* <button className='button bottomRightCorner'>Proceed</button> */}
        </div>
    </div>
  );
}