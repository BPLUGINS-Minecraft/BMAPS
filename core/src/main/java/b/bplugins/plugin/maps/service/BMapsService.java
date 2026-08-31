package b.bplugins.plugin.maps.service;

import b.bplugins.plugin.maps.command.PathUtil;
import b.bplugins.plugin.maps.model.MenuNode;
import b.bplugins.plugin.maps.model.WarpLocation;
import b.bplugins.plugin.maps.storage.NodeRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class BMapsService {

    private final NodeRepository repository;

    public BMapsService(NodeRepository repository) {
        this.repository = repository;
    }

    public MenuNode loadTree() throws SQLException {
        return repository.loadTree();
    }

    // ---------------------------------------------------------------
    // Anlegen
    // ---------------------------------------------------------------

    public String addCategoryPath(List<String> segments, String icon) throws SQLException, BMapsException {
        requireNonEmpty(segments, "Bitte einen Pfad angeben, z.B. Sachsen-Anhalt/Salzlandkreis");
        try {
            repository.resolveOrCreateCategoryPath(segments, icon);
        } catch (IllegalStateException e) {
            throw new BMapsException(e.getMessage());
        }
        return PathUtil.join(segments);
    }

    public String addWarp(List<String> pathSegments, String icon, WarpLocation location) throws SQLException, BMapsException {
        requireNonEmpty(pathSegments, "Bitte einen Pfad angeben, z.B. Sachsen-Anhalt/Salzlandkreis/Rathaus");

        String warpName = pathSegments.get(pathSegments.size() - 1);
        List<String> parentSegments = pathSegments.subList(0, pathSegments.size() - 1);

        if (parentSegments.isEmpty()) {
            throw new BMapsException("Warps müssen aktuell in einer Kategorie liegen, nicht direkt im Root. Erst eine Kategorie anlegen.");
        }

        NodeRepository.NodeRow parent = requireCategory(parentSegments);
        repository.insertWarp(parent.id(), warpName, icon, location);
        return PathUtil.join(pathSegments);
    }

    // ---------------------------------------------------------------
    // Ändern
    // ---------------------------------------------------------------

    public String updateIcon(List<String> segments, String expectedType, String icon) throws SQLException, BMapsException {
        requireNonEmpty(segments, "Bitte einen Pfad angeben.");
        NodeRepository.NodeRow node = requireNode(segments);

        if (!node.type().equals(expectedType)) {
            String actualLabel = node.type().equals("CATEGORY") ? "eine Kategorie" : "ein Warp";
            String commandHint = expectedType.equals("CATEGORY") ? "updatewarp" : "updatecategory";
            throw new BMapsException(PathUtil.join(segments) + " ist " + actualLabel + ". Nutze dafür den " + commandHint + "-Befehl.");
        }

        repository.updateIcon(node.id(), icon);
        return PathUtil.join(segments);
    }

    public String setDescription(List<String> segments, String description) throws SQLException, BMapsException {
        requireNonEmpty(segments, "Bitte einen Pfad angeben.");
        NodeRepository.NodeRow node = requireNode(segments);

        String finalDescription = (description == null || description.equals("-") || description.equalsIgnoreCase("clear"))
                ? null : description;
        repository.updateDescription(node.id(), finalDescription);
        return PathUtil.join(segments);
    }

    public String setPermission(List<String> segments, String permission) throws SQLException, BMapsException {
        requireNonEmpty(segments, "Bitte einen Pfad angeben.");
        NodeRepository.NodeRow node = requireNode(segments);

        String finalPermission = (permission == null || permission.equals("-") || permission.equalsIgnoreCase("clear"))
                ? null : permission;
        repository.updatePermission(node.id(), finalPermission);
        return PathUtil.join(segments);
    }

    public String move(List<String> fromSegments, List<String> toSegments) throws SQLException, BMapsException {
        requireNonEmpty(fromSegments, "Quellpfad fehlt.");
        requireNonEmpty(toSegments, "Zielpfad fehlt.");

        NodeRepository.NodeRow sourceNode = requireNode(fromSegments);

        String newName = toSegments.get(toSegments.size() - 1);
        List<String> destParentSegments = toSegments.subList(0, toSegments.size() - 1);

        Long newParentId = null;
        if (!destParentSegments.isEmpty()) {
            NodeRepository.NodeRow destParent = requireCategory(destParentSegments);
            newParentId = destParent.id();
        }

        if (sourceNode.type().equals("WARP") && newParentId == null) {
            throw new BMapsException("Warps müssen in einer Kategorie liegen, nicht direkt im Root.");
        }

        if (newParentId != null && repository.isSelfOrDescendant(sourceNode.id(), newParentId)) {
            throw new BMapsException("Kann nicht verschoben werden: Ziel liegt innerhalb von " + PathUtil.join(fromSegments) + " selbst.");
        }

        Optional<NodeRepository.NodeRow> collision = repository.findChild(newParentId, newName);
        if (collision.isPresent() && collision.get().id() != sourceNode.id()) {
            throw new BMapsException("Am Zielort existiert bereits ein Eintrag namens " + newName + ".");
        }

        repository.moveNode(sourceNode.id(), newParentId, newName);
        return PathUtil.join(toSegments);
    }

    // ---------------------------------------------------------------
    // Löschen
    // ---------------------------------------------------------------

    public RemoveResult removeNode(List<String> segments) throws SQLException, BMapsException {
        requireNonEmpty(segments, "Bitte einen Pfad angeben.");
        NodeRepository.NodeRow node = requireNode(segments);

        int descendants = repository.countDescendants(node.id());
        repository.deleteNode(node.id());
        return new RemoveResult(PathUtil.join(segments), descendants);
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden
    // ---------------------------------------------------------------

    private void requireNonEmpty(List<String> segments, String errorMessage) throws BMapsException {
        if (segments == null || segments.isEmpty()) {
            throw new BMapsException(errorMessage);
        }
    }

    private NodeRepository.NodeRow requireNode(List<String> segments) throws SQLException, BMapsException {
        Optional<NodeRepository.NodeRow> node = repository.resolvePath(segments);
        if (node.isEmpty()) {
            throw new BMapsException(PathUtil.join(segments) + " wurde nicht gefunden.");
        }
        return node.get();
    }

    private NodeRepository.NodeRow requireCategory(List<String> segments) throws SQLException, BMapsException {
        Optional<NodeRepository.NodeRow> node = repository.resolvePath(segments);
        if (node.isEmpty()) {
            throw new BMapsException("Kategorie " + PathUtil.join(segments) + " existiert nicht. Erst mit addcategory anlegen.");
        }
        if (!node.get().type().equals("CATEGORY")) {
            throw new BMapsException(PathUtil.join(segments) + " ist keine Kategorie.");
        }
        return node.get();
    }
}