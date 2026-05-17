package dk.ek.gruppe2.chooseyourfate.service.mongodb;

import dk.ek.gruppe2.chooseyourfate.dto.scene.CreateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.SceneResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.UpdateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.interfaces.SceneDataAccess;
import dk.ek.gruppe2.chooseyourfate.model.mongodb.SceneDocumentMongo;
import dk.ek.gruppe2.chooseyourfate.repository.mongodb.SceneRepositoryMongo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MongoSceneService implements SceneDataAccess {

    private final SceneRepositoryMongo sceneRepository;

    public MongoSceneService(SceneRepositoryMongo sceneRepository) {
        this.sceneRepository = sceneRepository;
    }

    @Override
    public List<SceneResponseDTO> getAllScenes() {
        List<SceneDocumentMongo> scenes = sceneRepository.findAll();
        /* TODO add dtos to return the data in the right format*/
        return null;
    }

    @Override
    public SceneResponseDTO getSceneById(Integer id) {
        return null;
    }

    public Optional<SceneDocumentMongo> getSceneWithNextScene(String id) {
        return sceneRepository.findByIdWithNextScenes(id);
    }

    @Override
    public SceneResponseDTO createScene(CreateSceneRequestDTO request) {
        return null;
    }

    @Override
    public SceneResponseDTO updateScene(Integer id, UpdateSceneRequestDTO request) {
        return null;
    }

    @Override
    public void deleteScene(Integer id) {

    }
}
